package org.example.mesh2.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.mesh2.auth.AuthService;
import org.example.mesh2.common.RequestContext;
import org.example.mesh2.mesh.MeshDtos.SendMessageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MeshWebSocketHandler extends TextWebSocketHandler {
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
        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText("");
        if (!"send".equalsIgnoreCase(type)) {
            return;
        }

        String token = root.path("token").asText(null);
        if (token == null || token.isBlank()) {
            Object queryToken = session.getAttributes().get("token");
            if (queryToken != null) {
                token = queryToken.toString();
            }
        }

        try {
            if (token != null && !token.isBlank()) {
                RequestContext.setCurrentUser(authService.resolveUser(token));
            }
            String content = root.path("content").asText("").trim();
            if (!content.isBlank()) {
                meshService.send(new SendMessageRequest(content));
            }
        } finally {
            RequestContext.clear();
        }
    }
}
