package pt.estga.processing.repositories.projections;

import java.time.Instant;

public interface SpatialClusterWithOccurrenceCount {
    Long getId();
    String getLabel();
    Double getCentroidLatitude();
    Double getCentroidLongitude();
    Instant getCreatedAt();
    Long getOccurrenceCount();
}
