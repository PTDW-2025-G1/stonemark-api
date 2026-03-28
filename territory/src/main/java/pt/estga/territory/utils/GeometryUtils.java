package pt.estga.territory.utils;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GeometryUtils {
    // SRID 4326 is WGS 84 (Standard GPS coordinates)
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    public static Point createPoint(Double lat, Double lon) {
        if (lat == null || lon == null) return null;
        return FACTORY.createPoint(new Coordinate(lon, lat)); // JTS uses (X, Y) -> (Lon, Lat)
    }
}