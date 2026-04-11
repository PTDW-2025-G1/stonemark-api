package pt.estga.processing.services.similarity.helpers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.estga.mark.repositories.MarkEvidenceRepository;
import pt.estga.mark.repositories.projections.EvidenceEmbeddingProjection;
import pt.estga.mark.repositories.projections.MarkEvidenceDistanceProjection;
import pt.estga.processing.config.policies.ParityPolicy;
import pt.estga.shared.utils.VectorUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Parity checker responsible for startup validation comparing DB similarity with
 * Java cosine similarity. Runs synchronously or asynchronously on a dedicated
 * single-thread daemon executor.
 */
@Service
@RequiredArgsConstructor
public class ParityChecker {

    private final MarkEvidenceRepository evidenceRepository;
    private final ParityPolicy parityPolicy;
    // Cached configuration values
    private boolean parityAsyncLocal;
    private int paritySampleSizeLocal;
    private double parityToleranceLocal;

    @PostConstruct
    void initLocalProperties() {
        this.parityAsyncLocal = parityPolicy.isAsync();
        this.paritySampleSizeLocal = Math.max(1, parityPolicy.getSampleSize());
        this.parityToleranceLocal = parityPolicy.getTolerance();
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
        var page = PageRequest.of(0, Math.max(1, paritySampleSizeLocal));
        var pageRes = evidenceRepository.findAllByEmbeddingIsNotNull(page);
        if (pageRes == null || pageRes.isEmpty()) return;
        for (var ev : pageRes.getContent()) {
            float[] emb = ev.getEmbedding();
            if (emb == null || emb.length == 0) continue;
            String vec = VectorUtils.toVectorLiteral(emb);
            List<MarkEvidenceDistanceProjection> hits = evidenceRepository.findTopKSimilarEvidence(vec, 5);
            if (hits == null || hits.isEmpty()) continue;
            List<UUID> hitIds = hits.stream().map(MarkEvidenceDistanceProjection::getId).filter(id -> !id.equals(ev.getId())).distinct().toList();
            if (hitIds.isEmpty()) continue;
            List<EvidenceEmbeddingProjection> fetched = evidenceRepository.findAllByIdIn(hitIds);
            Map<UUID, float[]> fetchedById = fetched.stream().collect(Collectors.toMap(
                    EvidenceEmbeddingProjection::getId,
                    EvidenceEmbeddingProjection::getEmbedding
            ));
            for (var p : hits) {
                if (p.getId().equals(ev.getId())) continue;
                Double dbSim = p.getSimilarity();
                if (dbSim == null) continue;
                float[] otherEmb = fetchedById.get(p.getId());
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
