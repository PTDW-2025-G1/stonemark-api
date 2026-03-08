package pt.estga.submission.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import pt.estga.file.entities.MediaFile;
import pt.estga.submission.enums.SubmissionType;
import pt.estga.shared.utils.PgVectorType;

@Entity
@DiscriminatorValue("MARK")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class MarkSubmission extends Submission {

    private String description;

    @Type(PgVectorType.class)
    @Column(columnDefinition = "vector")
    private float[] embedding;

    @OneToOne(cascade = CascadeType.ALL)
    private MediaFile coverImage;

    @Override
    public SubmissionType getType() {
        return SubmissionType.MARK;
    }
}
