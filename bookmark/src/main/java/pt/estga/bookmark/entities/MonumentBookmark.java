package pt.estga.bookmark.entities;

import jakarta.persistence.*;
import lombok.*;
import pt.estga.monument.Monument;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MonumentBookmark extends BaseBookmark {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Monument monument;
}
