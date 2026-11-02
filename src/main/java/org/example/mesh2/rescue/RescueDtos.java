package org.example.mesh2.rescue;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class RescueDtos {
    private RescueDtos() {}

    public record CreateTeamRequest(
            @NotBlank(message = "Team name is required.") @Size(max = 120, message = "Team name must be 120 characters or fewer.") String name,
            @NotBlank(message = "Specialty is required.") @Size(max = 120, message = "Specialty must be 120 characters or fewer.") String specialty,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude,
            @Min(value = 1, message = "Team size must be at least one.")
            @Max(value = 100, message = "Team size cannot exceed 100.") Integer memberCount
    ) {}

    public record AssignRequest(
            @NotNull(message = "SOS alert id is required.") Long sosAlertId,
            @NotNull(message = "Team id is required.") Long teamId,
            String notes
    ) {}

    public record UpdateAssignmentStatusRequest(@NotBlank(message = "Status is required.") String status) {}

    public record TeamResponse(
            Long id,
            String name,
            String specialty,
            String status,
            Double latitude,
            Double longitude,
            int memberCount
    ) {
        public static TeamResponse from(RescueTeam team) {
            return new TeamResponse(
                    team.getId(),
                    team.getName(),
                    team.getSpecialty(),
                    team.getStatus(),
                    team.getLatitude(),
                    team.getLongitude(),
                    team.getMemberCount()
            );
        }
    }

    public record AssignmentResponse(
            Long id,
            Long sosAlertId,
            String sosPublicCode,
            Long teamId,
            String teamName,
            String category,
            String notes,
            String status,
            String severity,
            String createdAt,
            String updatedAt
    ) {}
}
