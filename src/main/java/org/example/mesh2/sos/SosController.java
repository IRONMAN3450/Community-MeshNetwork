package org.example.mesh2.sos;

import jakarta.validation.Valid;
import java.util.List;
import org.example.mesh2.sos.SosDtos.CreateSosRequest;
import org.example.mesh2.sos.SosDtos.SosResponse;
import org.example.mesh2.sos.SosDtos.UpdateSosStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sos")
public class SosController {
    private final SosService sosService;

    public SosController(SosService sosService) {
        this.sosService = sosService;
    }

    @PostMapping
    public ResponseEntity<SosResponse> create(@Valid @RequestBody CreateSosRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sosService.create(request));
    }

    @GetMapping
    public List<SosResponse> listAll() {
        return sosService.listAll();
    }

    @GetMapping("/active")
    public List<SosResponse> listActive() {
        return sosService.listActive();
    }

    @GetMapping("/mine")
    public List<SosResponse> listMine() {
        return sosService.listMine();
    }

    @GetMapping("/{id}")
    public SosResponse get(@PathVariable Long id) {
        return sosService.getById(id);
    }

    @PutMapping("/{id}/status")
    public SosResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateSosStatusRequest request) {
        return sosService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/advance")
    public SosResponse advance(@PathVariable Long id) {
        return sosService.advanceStatus(id);
    }
}
