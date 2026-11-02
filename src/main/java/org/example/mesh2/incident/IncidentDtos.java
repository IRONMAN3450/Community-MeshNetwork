package org.example.mesh2.incident;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class IncidentDtos {
    private IncidentDtos() {}

    public record CreateIncidentRequest(
            @NotBlank(message = "Title is required.") @Size(max = 160, message = "Title must be 160 characters or fewer.") String title,
            @NotBlank(message = "Type is required.") @Size(max = 50, message = "Type must be 50 characters or fewer.") String type,
            @NotBlank(message = "Sector is required.") @Size(max = 160, message = "Sector must be 160 characters or fewer.") String sector,
            @Size(max = 2000, message = "Description must be 2000 characters or fewer.") String description,
            String severity,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude
    ) {}

    public record UpdateIncidentRequest(
            @Size(max = 160, message = "Title must be 160 characters or fewer.") String title,
            @Size(max = 50, message = "Type must be 50 characters or fewer.") String type,
            @Size(max = 160, message = "Sector must be 160 characters or fewer.") String sector,
            @Size(max = 2000, message = "Description must be 2000 characters or fewer.") String description,
            @Size(max = 20, message = "Severity must be 20 characters or fewer.") String severity,
            @Size(max = 20, message = "Status must be 20 characters or fewer.") String status,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude
    ) {}

    public record IncidentResponse(
            Long id,
            String title,
            String type,
            String sector,
            String description,
            String severity,
            String status,
            Double latitude,
            Double longitude,
            long activeSosCount,
            long responseTeamCount,
            String createdAt,
            String updatedAt
    ) {
        public static IncidentResponse from(Incident incident) {
            return new IncidentResponse(
                    incident.getId(),
                    incident.getTitle(),
                    incident.getType(),
                    incident.getSector(),
                    incident.getDescription(),
                    incident.getSeverity(),
                    incident.getStatus(),
                    incident.getLatitude(),
                    incident.getLongitude(),
                    incident.getActiveSosCount(),
                    incident.getResponseTeamCount(),
                    incident.getCreatedAt() == null ? null : incident.getCreatedAt().toString(),
                    incident.getUpdatedAt() == null ? null : incident.getUpdatedAt().toString()
            );
        }
    }
}
