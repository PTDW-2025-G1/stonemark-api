package pt.estga.bookmark.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MarkOccurrenceBookmark extends BaseBookmark {

    @Column(nullable = false)
    private Long markOccurrenceId;
}
