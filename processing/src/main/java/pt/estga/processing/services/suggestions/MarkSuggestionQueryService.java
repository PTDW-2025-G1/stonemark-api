package pt.estga.processing.services.suggestions;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.dtos.MarkSuggestionDto;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.mappers.MarkSuggestionMapper;
import pt.estga.processing.repositories.MarkSuggestionRepository;
import pt.estga.sharedweb.filtering.QueryProcessor;
import pt.estga.sharedweb.models.PagedRequest;
import pt.estga.sharedweb.models.QueryResult;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarkSuggestionQueryService {

    private final MarkSuggestionRepository markSuggestionRepository;
    private final QueryProcessor<MarkSuggestion> queryProcessor;
    private final MarkSuggestionMapper mapper;

    public Page<MarkSuggestion> search(PagedRequest request) {
        QueryResult<MarkSuggestion> result = queryProcessor.process(request);

        Page<MarkSuggestion> page = markSuggestionRepository.findAll(
                result.specification(),
                result.pageable()
        );

        return page;
    }

    public Optional<MarkSuggestion> findById(UUID id) {
        return markSuggestionRepository.findById(id);
    }

    public Optional<MarkSuggestionDto> findDtoById(UUID id) {
        return findById(id).map(mapper::toDto);
    }
}
