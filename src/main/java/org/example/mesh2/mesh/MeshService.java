package org.example.mesh2.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.common.RequestContext;
import org.example.mesh2.mesh.MeshDtos.MessageResponse;
import org.example.mesh2.mesh.MeshDtos.SendMessageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeshService {
    private final MeshMessageRepository messages;
    private final MeshWebSocketHub webSocketHub;

    public MeshService(MeshMessageRepository messages, MeshWebSocketHub webSocketHub) {
        this.messages = messages;
        this.webSocketHub = webSocketHub;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> history() {
        List<MeshMessage> latest = new ArrayList<>(messages.findTop100ByOrderByCreatedAtDesc());
        Collections.reverse(latest);
        return latest.stream().map(MessageResponse::from).toList();
    }

    @Transactional
    public MessageResponse send(SendMessageRequest request) {
        UserAccount sender = RequestContext.getCurrentUser();
        String senderName = sender == null ? "Anonymous Node" : sender.getFullName();
        Long senderId = sender == null ? null : sender.getId();
        MeshMessage saved = messages.save(new MeshMessage(
                senderId,
                senderName,
                request.content().trim(),
                webSocketHub.connectedCount() > 0 ? "DELIVERED" : "STORED",
                1
        ));
        MessageResponse response = MessageResponse.from(saved);
        webSocketHub.broadcast(response);
        return response;
    }

    public int connectedClients() {
        return webSocketHub.connectedCount();
    }
}
