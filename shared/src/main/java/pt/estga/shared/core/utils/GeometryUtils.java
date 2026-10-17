package pt.estga.shared.core.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Builds JTS geometries for WGS84 (SRID 4326) coordinates shared across modules.
 */
public final class GeometryUtils {

    private static final int SRID_WGS84 = 4326;
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeometryUtils() {
    }

    /**
     * Creates a point from a latitude/longitude pair, or {@code null} when either
     * coordinate is absent (an occurrence without a location is valid).
     */
    public static Point createPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        // JTS coordinates are ordered (X, Y), i.e. (longitude, latitude).
        return FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
