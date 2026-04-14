package pt.estga.review.dtos;

import org.locationtech.jts.geom.Point;

/**
 * Carries the user's input for resolving identities during a review.
 */
public record DiscoveryContext(
        String markTitle,
        Long existingMarkId,
        String monumentName,
        Long existingMonumentId,
        Point location
) {
    public boolean isNewMark() { return existingMarkId == null && markTitle != null; }
    public boolean isNewMonument() { return existingMonumentId == null && monumentName != null; }
}
