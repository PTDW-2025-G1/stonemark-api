package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.mark.entities.MarkOccurrence;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrenceBookmark extends BaseBookmark {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MarkOccurrence markOccurrence;
}
