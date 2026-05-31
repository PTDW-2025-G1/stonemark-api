package pt.estga.review.dtos;

import pt.estga.processing.repositories.projections.SpatialClusterWithOccurrenceCount;

import java.time.Instant;

public record SpatialClusterListDto(
        Long id,
        String label,
        Double centroidLatitude,
        Double centroidLongitude,
        Instant createdAt,
        Long occurrenceCount) {

    public static SpatialClusterListDto from(SpatialClusterWithOccurrenceCount projection) {
        return new SpatialClusterListDto(
                projection.getId(),
                projection.getLabel(),
                projection.getCentroidLatitude(),
                projection.getCentroidLongitude(),
                projection.getCreatedAt(),
                projection.getOccurrenceCount());
    }
}
