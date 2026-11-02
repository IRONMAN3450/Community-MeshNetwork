package org.example.mesh2.mesh;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.example.mesh2.mesh.MeshDtos.MessageResponse;
import org.example.mesh2.mesh.MeshDtos.SendMessageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mesh")
public class MeshController {
    private final MeshService meshService;

    public MeshController(MeshService meshService) {
        this.meshService = meshService;
    }

    @GetMapping("/messages")
    public List<MessageResponse> history() {
        return meshService.history();
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meshService.send(request));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "connectedClients", meshService.connectedClients(),
                "channel", "/ws/mesh"
        );
    }
}
