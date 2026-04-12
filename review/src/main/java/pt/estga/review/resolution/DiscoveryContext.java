package pt.estga.review.resolution;

import org.locationtech.jts.geom.Point;

/**
 * Context provided when attempting to discover or resolve an identity during review.
 * All fields are nullable to allow partial discovery input.
 */
public record DiscoveryContext(
        String markTitle,
        Long existingMarkId,
        String monumentTitle,
        Long existingMonumentId,
        Point coordinates
) {
}
