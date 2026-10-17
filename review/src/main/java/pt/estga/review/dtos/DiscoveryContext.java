package pt.estga.review.dtos;

import java.util.UUID;

import org.locationtech.jts.geom.Point;

/**
 * Carries the user's input for resolving identities during a review.
 */
public record DiscoveryContext(
        String markTitle,
        UUID existingMarkId,
        String monumentName,
        UUID existingMonumentId,
        Point location
) {
    public boolean isNewMark() { return existingMarkId == null && markTitle != null; }
    public boolean isNewMonument() { return existingMonumentId == null && monumentName != null; }
}
