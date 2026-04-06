package pt.estga.processing.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;
import pt.estga.mark.entities.Mark;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkSuggestion {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private MarkEvidenceProcessing processing;

    @ManyToOne
    private Mark mark;

    private double confidence;

    private boolean isNewMarkSuggestion;
}