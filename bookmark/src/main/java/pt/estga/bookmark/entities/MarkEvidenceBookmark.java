package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.mark.entities.MarkEvidence;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkEvidenceBookmark extends BaseBookmark {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MarkEvidence markEvidence;
}
