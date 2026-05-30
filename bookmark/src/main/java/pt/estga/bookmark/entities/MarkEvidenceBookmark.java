package pt.estga.bookmark.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MarkEvidenceBookmark extends BaseBookmark {

    @Column(nullable = false)
    private UUID markEvidenceId;
}
