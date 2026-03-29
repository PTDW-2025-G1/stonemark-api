package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import pt.estga.mark.entities.Mark;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MarkBookmark extends BaseBookmark {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Mark mark;
}
