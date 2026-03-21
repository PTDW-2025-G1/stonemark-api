package pt.estga.report.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pt.estga.content.enums.TargetType;
import pt.estga.report.enums.ReportReason;

public record ReportRequestDto(
        @NotNull
        Long targetId,
        @NotNull
        TargetType targetType,
        @NotNull(message = "Reason is required")
        ReportReason reason,
        @Size(min = 10, max = 1000, message = "Description must be at most 1000 characters")
        String description
) {}

