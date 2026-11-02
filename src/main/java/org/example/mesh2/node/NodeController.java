package org.example.mesh2.node;

import jakarta.validation.Valid;
import java.util.List;
import org.example.mesh2.node.NodeDtos.HeartbeatRequest;
import org.example.mesh2.node.NodeDtos.NodeResponse;
import org.example.mesh2.node.NodeDtos.RegisterNodeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {
    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    public ResponseEntity<NodeResponse> register(@Valid @RequestBody RegisterNodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.register(request));
    }

    @GetMapping
    public List<NodeResponse> list() {
        return nodeService.list();
    }

    @PostMapping("/{id}/heartbeat")
    public NodeResponse heartbeat(@PathVariable Long id, @Valid @RequestBody HeartbeatRequest request) {
        return nodeService.heartbeat(id, request);
    }
}
