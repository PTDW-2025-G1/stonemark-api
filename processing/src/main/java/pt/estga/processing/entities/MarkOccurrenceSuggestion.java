package pt.estga.processing.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;
import pt.estga.monument.Monument;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkOccurrenceSuggestion {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private MarkSuggestion suggestion;

    @ManyToOne
    private Monument monument;

    private double distanceScore;
}