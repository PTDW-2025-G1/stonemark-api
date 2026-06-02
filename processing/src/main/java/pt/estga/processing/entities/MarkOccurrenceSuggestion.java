package pt.estga.processing.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

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

    private Long monumentId;

    private double distanceScore;
}