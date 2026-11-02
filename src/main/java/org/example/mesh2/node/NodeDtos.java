package org.example.mesh2.node;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public final class NodeDtos {
    private NodeDtos() {}

    public record RegisterNodeRequest(
            @NotBlank(message = "Display name is required.")
            @Size(max = 120, message = "Display name must be 120 characters or fewer.") String displayName,
            @Size(max = 50, message = "Node type must be 50 characters or fewer.") String nodeType,
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude
    ) {}

    public record HeartbeatRequest(
            @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90.")
            @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90.") Double latitude,
            @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180.")
            @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180.") Double longitude,
            @Size(max = 20, message = "Status must be 20 characters or fewer.") String status
    ) {}

    public record NodeResponse(
            Long id,
            String nodeCode,
            String displayName,
            String nodeType,
            Long ownerUserId,
            Double latitude,
            Double longitude,
            String status,
            String lastSeenAt
    ) {
        public static NodeResponse from(NetworkNode node) {
            return new NodeResponse(
                    node.getId(),
                    node.getNodeCode(),
                    node.getDisplayName(),
                    node.getNodeType(),
                    node.getOwnerUserId(),
                    node.getLatitude(),
                    node.getLongitude(),
                    node.getStatus(),
                    node.getLastSeenAt() == null ? null : node.getLastSeenAt().toString()
            );
        }
    }
}
