package pt.estga.processing.services.suggestions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.processing.entities.MarkSuggestion;
import pt.estga.processing.repositories.MarkSuggestionRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkSuggestionCommandService {

    private final MarkSuggestionRepository markSuggestionRepository;

    @Transactional
    public MarkSuggestion create(MarkSuggestion suggestion) {
        Objects.requireNonNull(suggestion, "suggestion must not be null");
        return doCreate(suggestion);
    }

    @Transactional
    public MarkSuggestion update(MarkSuggestion suggestion) {
        Objects.requireNonNull(suggestion, "suggestion must not be null");
        Objects.requireNonNull(suggestion.getId(), "suggestion.id must not be null");
        return doUpdate(suggestion);
    }

    @Transactional
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        doDelete(id);
    }

    @Transactional
    public void deleteByProcessingId(UUID processingId) {
        Objects.requireNonNull(processingId, "processingId must not be null");
        markSuggestionRepository.deleteByProcessingId(processingId);
        log.debug("Deleted suggestions for processing {}", processingId);
    }

    // --- internal helpers -------------------------------------------------

    private MarkSuggestion doCreate(MarkSuggestion suggestion) {
        // ensure we create a new entity
        suggestion.setId(null);
        MarkSuggestion saved = markSuggestionRepository.save(suggestion);
        log.debug("Created MarkSuggestion {} for processing {} and mark {}",
                saved.getId(),
                saved.getProcessing() == null ? null : saved.getProcessing().getId(),
                saved.getMark() == null ? null : saved.getMark().getId());
        return saved;
    }

    /**
     * Persist a batch of suggestions in a single transaction. Existing ids on the provided
     * entities will be ignored and replaced by newly generated ids.
     *
     * @param suggestions list of suggestions to persist (must not be null)
     * @return list of persisted suggestions
     */
    @Transactional
    public List<MarkSuggestion> createAll(List<MarkSuggestion> suggestions) {
        Objects.requireNonNull(suggestions, "suggestions must not be null");
        // Ensure we create new rows
        suggestions.forEach(s -> s.setId(null));
        List<MarkSuggestion> saved = markSuggestionRepository.saveAll(suggestions);
        log.debug("Created {} MarkSuggestions", saved.size());
        return saved;
    }

    private MarkSuggestion doUpdate(MarkSuggestion suggestion) {
        Optional<MarkSuggestion> opt = markSuggestionRepository.findById(suggestion.getId());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("MarkSuggestion not found: " + suggestion.getId());
        }
        MarkSuggestion existing = opt.get();
        // update allowed fields
        existing.setConfidence(suggestion.getConfidence());
        existing.setMark(suggestion.getMark());
        existing.setProcessing(suggestion.getProcessing());
        MarkSuggestion saved = markSuggestionRepository.save(existing);
        log.debug("Updated MarkSuggestion {} (confidence={})", saved.getId(), saved.getConfidence());
        return saved;
    }

    private void doDelete(UUID id) {
        if (!markSuggestionRepository.existsById(id)) {
            log.debug("MarkSuggestion {} not found, nothing to delete", id);
            return;
        }
        markSuggestionRepository.deleteById(id);
        log.debug("Deleted MarkSuggestion {}", id);
    }

}
