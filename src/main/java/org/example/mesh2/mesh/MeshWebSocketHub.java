package org.example.mesh2.mesh;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.mesh2.mesh.MeshDtos.MessageResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MeshWebSocketHub {
    private static final Logger logger = LoggerFactory.getLogger(MeshWebSocketHub.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public MeshWebSocketHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public int connectedCount() {
        sessions.removeIf(session -> !session.isOpen());
        return sessions.size();
    }

    public void broadcast(MessageResponse message) {
        sessions.removeIf(session -> !session.isOpen());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "type", "mesh-message",
                    "payload", message
            ));
        } catch (IOException ex) {
            logger.error("Could not serialize mesh message for WebSocket broadcast", ex);
            return;
        }
        TextMessage textMessage = new TextMessage(payload);
        for (WebSocketSession session : sessions) {
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (IOException ignored) {
                unregister(session);
                logger.debug("Removing a WebSocket session that could not receive a mesh message", ignored);
            }
        }
    }
}
