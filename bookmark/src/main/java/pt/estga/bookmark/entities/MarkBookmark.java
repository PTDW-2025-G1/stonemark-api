package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.mark.entities.Mark;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkBookmark extends BaseBookmark {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Mark mark;
}
