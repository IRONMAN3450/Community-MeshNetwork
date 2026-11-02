package org.example.mesh2.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.auth.AuthService;
import org.example.mesh2.common.RequestContext;
import org.example.mesh2.mesh.MeshDtos.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MeshWebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(MeshWebSocketHandler.class);
    private final MeshWebSocketHub hub;
    private final MeshService meshService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public MeshWebSocketHandler(MeshWebSocketHub hub, MeshService meshService, AuthService authService,
                                ObjectMapper objectMapper) {
        this.hub = hub;
        this.meshService = meshService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        hub.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.unregister(session);
        RequestContext.clear();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            if (!"send".equalsIgnoreCase(type)) {
                sendFrame(session, Map.of("type", "error", "message", "Unsupported message type."));
                return;
            }

            String token = root.path("token").asText(null);
            if (token == null || token.isBlank()) {
                Object queryToken = session.getAttributes().get("token");
                if (queryToken != null) token = queryToken.toString();
            }
            if (token != null && !token.isBlank()) {
                RequestContext.setCurrentUser(authService.resolveUser(token));
            }
            String content = root.path("content").asText("").trim();
            if (content.isBlank() || content.length() > 1000) {
                sendFrame(session, Map.of("type", "error", "message", "Message must contain 1 to 1000 characters."));
                return;
            }
            var saved = meshService.send(new SendMessageRequest(content));
            sendFrame(session, Map.of("type", "sent", "payload", saved));
        } catch (Exception ex) {
            logger.warn("Could not process a WebSocket message: {}", ex.getMessage());
            String error = ex instanceof ApiException ? ex.getMessage() : "Could not process the message. Please try again.";
            sendFrame(session, Map.of("type", "error", "message", error));
        } finally {
            RequestContext.clear();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        hub.unregister(session);
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    private void sendFrame(WebSocketSession session, Object payload) throws Exception {
        if (!session.isOpen()) return;
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }
}
