package pt.estga.processing.services.similarity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.estga.mark.dtos.MarkEvidenceDistanceDto;
import pt.estga.mark.dtos.MarkEvidenceDto;
import pt.estga.mark.mappers.MarkEvidenceMapper;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.markapi.MarkEvidenceQueryService;
import pt.estga.processing.config.ProcessingProperties;
import pt.estga.commoncore.utils.VectorUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParityChecker {

    private final MarkEvidenceQueryService markEvidenceQueryService;
    private final MarkEvidenceRepository markEvidenceRepository;
    private final ProcessingProperties properties;
    private boolean parityAsyncLocal;
    private int paritySampleSizeLocal;
    private double parityToleranceLocal;

    @PostConstruct
    void initLocalProperties() {
        var pp = properties.similarity().parityCheck();
        this.parityAsyncLocal = pp.async();
        this.paritySampleSizeLocal = Math.max(1, pp.sampleSize());
        this.parityToleranceLocal = pp.tolerance();
    }

    private static final ExecutorService parityExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "similarity-parity-check");
        t.setDaemon(true);
        return t;
    });

    public void maybeRun() {
        if (parityAsyncLocal) {
            parityExecutor.submit(() -> { try { run(); } catch (Exception e) { /* log at caller */ } });
        } else {
            run();
        }
    }

    public void run() {
        List<MarkEvidenceDto> sample = markEvidenceQueryService.findEvidenceWithEmbeddings(Math.max(1, paritySampleSizeLocal));
        if (sample == null || sample.isEmpty()) return;
        for (var ev : sample) {
            float[] emb = ev.embedding();
            if (emb == null || emb.length == 0) continue;
            String vec = VectorUtils.toVectorLiteral(emb);
            List<MarkEvidenceDistanceDto> hits = markEvidenceQueryService.findTopKSimilar(vec, 5);
            if (hits == null || hits.isEmpty()) continue;
            List<UUID> hitIds = hits.stream().map(MarkEvidenceDistanceDto::id).filter(id -> !id.equals(ev.id())).distinct().toList();
            if (hitIds.isEmpty()) continue;
            List<MarkEvidenceDto> fetched = markEvidenceRepository.findAllById(hitIds).stream()
                    .map(MarkEvidenceMapper::toDto)
                    .toList();
            Map<UUID, float[]> fetchedById = fetched.stream().collect(Collectors.toMap(
                    MarkEvidenceDto::id,
                    MarkEvidenceDto::embedding
            ));
            for (var p : hits) {
                if (p.id().equals(ev.id())) continue;
                Double dbSim = p.similarity();
                if (dbSim == null) continue;
                float[] otherEmb = fetchedById.get(p.id());
                if (otherEmb == null) continue;
                Double javaCos = VectorUtils.cosineSimilarity(emb, otherEmb);
                if (javaCos == null) continue;
                double diff = Math.abs(dbSim - javaCos);
                double parityTolerance = parityToleranceLocal;
                if (diff > parityTolerance) {
                    throw new IllegalStateException(String.format("DB/Java similarity mismatch: db=%.6f java=%.6f diff=%.6f (tolerance=%.6f).", dbSim, javaCos, diff, parityTolerance));
                }
                break;
            }
        }
    }
}
