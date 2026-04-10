package pt.estga.mark.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.estga.mark.entities.Mark;
import pt.estga.mark.entities.MarkOccurrence;
import pt.estga.mark.services.mark.MarkCommandService;
import pt.estga.mark.services.occurrence.MarkOccurrenceCommandService;

/**
 * Encapsulates creation logic for Mark and MarkOccurrence derived from a draft.
 * This is the central place to add deduplication, category assignment and AI-driven suggestions.
 */
@Service
@RequiredArgsConstructor
public class MarkCreationService {

    private final MarkCommandService markCommandService;
    private final MarkOccurrenceCommandService occurrenceCommandService;

    @Transactional
    public MarkOccurrence createFromDraft(String titleHint, float[] embedding) {
        // Minimal implementation: create a new Mark and a new occurrence linked to it.
        Mark mark = Mark.builder()
                .title(titleHint != null && !titleHint.isBlank() ? titleHint : "New mark")
                .description(null)
                .build();

        mark = markCommandService.create(mark);

        MarkOccurrence occurrence = MarkOccurrence.builder()
                .mark(mark)
                .build();

        occurrence = occurrenceCommandService.create(occurrence);
        return occurrence;
    }
}

