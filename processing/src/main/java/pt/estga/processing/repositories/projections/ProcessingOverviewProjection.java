package pt.estga.processing.repositories.projections;

import pt.estga.processing.enums.ProcessingStatus;

import java.util.UUID;

public interface ProcessingOverviewProjection {
	UUID getId();
	ProcessingStatus getStatus();
}
