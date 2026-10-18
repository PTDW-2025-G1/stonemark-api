package pt.estga.review.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import pt.estga.review.enums.MarkCreationSuggestionStatus;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A cluster of submissions that matched no existing mark but resemble each other.
 * When a cluster reaches the configured size, it is proposed to moderators as a
 * candidate for a new mark; no mark is created until a moderator acts on it.
 */
@Entity
@Table(name = "mark_creation_suggestion")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MarkCreationSuggestion {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MarkCreationSuggestionStatus status;

    /** Number of submissions in the cluster. */
    private int size;

    /** Average cosine similarity of the members to the cluster centroid. */
    private double coherence;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mark_creation_suggestion_member", joinColumns = @JoinColumn(name = "suggestion_id"))
    @Column(name = "submission_id", nullable = false)
    @Builder.Default
    private Set<UUID> memberSubmissionIds = new LinkedHashSet<>();

    /** The mark created from this cluster, once actioned. */
    private UUID createdMarkId;

    private Instant createdAt;

    private Instant updatedAt;
}
