package org.example.mesh2.node;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.account.UserRole;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.common.RequestContext;
import org.example.mesh2.node.NodeDtos.HeartbeatRequest;
import org.example.mesh2.node.NodeDtos.NodeResponse;
import org.example.mesh2.node.NodeDtos.RegisterNodeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NodeService {
    private final NetworkNodeRepository nodes;

    public NodeService(NetworkNodeRepository nodes) {
        this.nodes = nodes;
    }

    @Transactional
    public NodeResponse register(RegisterNodeRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        UserAccount user = RequestContext.requireCurrentUser();
        String code = "NODE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        NetworkNode node = new NetworkNode(
                code,
                request.displayName().trim(),
                blankToDefault(request.nodeType(), "DEVICE"),
                user.getId(),
                request.latitude() != null ? request.latitude() : user.getLatitude(),
                request.longitude() != null ? request.longitude() : user.getLongitude()
        );
        node.setStatus("ONLINE");
        return NodeResponse.from(nodes.save(node));
    }

    @Transactional(readOnly = true)
    public List<NodeResponse> list() {
        return nodes.findByOrderByCreatedAtDesc().stream().map(NodeResponse::from).toList();
    }

    @Transactional
    public NodeResponse heartbeat(Long id, HeartbeatRequest request) {
        validateCoordinates(request.latitude(), request.longitude());
        NetworkNode node = nodes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Network node not found."));
        UserAccount user = RequestContext.requireCurrentUser();
        if (!user.getId().equals(node.getOwnerUserId())
                && user.getRole() != UserRole.RESPONDER && user.getRole() != UserRole.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the node owner or a responder can update this node.");
        }
        if (request.latitude() != null) {
            node.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            node.setLongitude(request.longitude());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String status = request.status().trim().toUpperCase(java.util.Locale.ROOT);
            if (!List.of("ONLINE", "OFFLINE", "MAINTENANCE").contains(status)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid network node status.");
            }
            node.setStatus(status);
        } else {
            node.setStatus("ONLINE");
        }
        node.setLastSeenAt(Instant.now());
        return NodeResponse.from(nodes.save(node));
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
    }
}
