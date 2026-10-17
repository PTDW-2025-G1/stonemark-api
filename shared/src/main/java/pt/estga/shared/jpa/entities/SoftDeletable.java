package pt.estga.shared.jpa.entities;

/**
 * Contract for entities that are removed by marking a timestamp rather than by
 * deleting the row, so that referencing rows stay valid.
 */
public interface SoftDeletable {

    void markDeleted();

    boolean isDeleted();
}
