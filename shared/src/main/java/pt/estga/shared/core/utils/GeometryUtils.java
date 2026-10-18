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

    /**
     * Great-circle distance in metres between two WGS84 coordinates, used to decide
     * whether a finding refers to the same place as an existing one.
     */
    public static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusMeters = 6371000.0;
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return 2 * earthRadiusMeters * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }
}
