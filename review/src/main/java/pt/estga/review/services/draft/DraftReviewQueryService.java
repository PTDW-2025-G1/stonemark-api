package pt.estga.review.services.draft;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pt.estga.review.repositories.DraftReviewRepository;

@Service
@RequiredArgsConstructor
public class DraftReviewQueryService {

    private final DraftReviewRepository repository;

    public boolean existsByDraftId(Long draftId) {
        return repository.existsByDraftId(draftId);
    }
}
