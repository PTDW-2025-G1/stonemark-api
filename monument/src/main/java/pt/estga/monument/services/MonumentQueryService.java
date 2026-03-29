package pt.estga.monument.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.monument.Monument;
import pt.estga.monument.MonumentRepository;
import pt.estga.monument.dots.MonumentListDto;
import pt.estga.monument.MonumentMapper;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonumentQueryService {

    private final MonumentRepository repository;
    private final QueryProcessor<Monument> queryProcessor;
    private final MonumentMapper mapper;

    public Optional<Monument> findById(Long id) {
        return repository.findById(id);
    }

    public Page<MonumentListDto> search(PagedRequest request) {
        QueryResult<Monument> result = queryProcessor.process(request);

        Page<Monument> monuments = repository.findAll(
                result.specification(),
                result.pageable()
        );

        return monuments.map(mapper::toListDto);
    }

    public Page<Monument> findByPolygon(String geoJson, Pageable pageable) {
        return repository.findByPolygon(geoJson, pageable);
    }

    public Page<Monument> findByDivisionId(Long id, Pageable pageable) {
        return repository.findByDivisionId(id, pageable);
    }
}
