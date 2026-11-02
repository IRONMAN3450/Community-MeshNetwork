package org.example.mesh2.sos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class SosDtos {
    private SosDtos() {}

    public record CreateSosRequest(
            @NotBlank(message = "Emergency category is required.")
            @Size(max = 80, message = "Emergency category must be 80 characters or fewer.") String category,
            @Size(max = 1000, message = "Description must be 1000 characters or fewer.") String description,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude,
            @Size(max = 200, message = "Location label must be 200 characters or fewer.") String locationLabel,
            @Size(max = 20, message = "Severity must be 20 characters or fewer.") String severity,
            @Positive(message = "Incident id must be positive.") Long incidentId
    ) {}

    public record UpdateSosStatusRequest(@NotBlank(message = "Status is required.") String status) {}

    public record SosResponse(
            Long id,
            String publicCode,
            Long reporterId,
            String reporterName,
            String category,
            String description,
            Double latitude,
            Double longitude,
            String locationLabel,
            String status,
            String severity,
            Long incidentId,
            Long assignedTeamId,
            String assignedTeamName,
            String createdAt,
            String updatedAt,
            String resolvedAt
    ) {
        public static SosResponse from(SosAlert alert) {
            return new SosResponse(
                    alert.getId(),
                    alert.getPublicCode(),
                    alert.getReporterId(),
                    alert.getReporterName(),
                    alert.getCategory(),
                    alert.getDescription(),
                    alert.getLatitude(),
                    alert.getLongitude(),
                    alert.getLocationLabel(),
                    alert.getStatus().name(),
                    alert.getSeverity(),
                    alert.getIncidentId(),
                    alert.getAssignedTeamId(),
                    alert.getAssignedTeamName(),
                    alert.getCreatedAt() == null ? null : alert.getCreatedAt().toString(),
                    alert.getUpdatedAt() == null ? null : alert.getUpdatedAt().toString(),
                    alert.getResolvedAt() == null ? null : alert.getResolvedAt().toString()
            );
        }
    }
}
