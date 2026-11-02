package org.example.mesh2.mesh;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MeshDtos {
    private MeshDtos() {}

    public record SendMessageRequest(
            @NotBlank(message = "Message content is required.")
            @Size(max = 1000, message = "Message must be 1000 characters or fewer.") String content
    ) {}

    public record MessageResponse(
            Long id,
            Long senderId,
            String senderName,
            String content,
            String deliveryStatus,
            int hopCount,
            String createdAt
    ) {
        public static MessageResponse from(MeshMessage message) {
            return new MessageResponse(
                    message.getId(),
                    message.getSenderId(),
                    message.getSenderName(),
                    message.getContent(),
                    message.getDeliveryStatus(),
                    message.getHopCount(),
                    message.getCreatedAt() == null ? null : message.getCreatedAt().toString()
            );
        }
    }
}
