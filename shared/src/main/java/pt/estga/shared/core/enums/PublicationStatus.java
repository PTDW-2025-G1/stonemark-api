package pt.estga.shared.core.enums;

/**
 * Lifecycle of a publishable entity.
 *
 * <p>Replaces the previous overlap of {@link EntityStatus} (a live/soft-delete flag)
 * and {@code ValidationState} (an approval flag) on publishable entities with a single
 * notion of lifecycle. Soft deletion stays a separate concern.
 */
public enum PublicationStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
