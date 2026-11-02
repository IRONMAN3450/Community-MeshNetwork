package org.example.mesh2.incident;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.example.mesh2.incident.IncidentDtos.CreateIncidentRequest;
import org.example.mesh2.incident.IncidentDtos.IncidentResponse;
import org.example.mesh2.incident.IncidentDtos.UpdateIncidentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @GetMapping
    public List<IncidentResponse> list(@RequestParam(required = false) String type) {
        return incidentService.listAll(type);
    }

    @GetMapping("/{id}")
    public IncidentResponse get(@PathVariable Long id) {
        return incidentService.get(id);
    }

    @PutMapping("/{id}")
    public IncidentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateIncidentRequest request) {
        return incidentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> archive(@PathVariable Long id) {
        incidentService.archive(id);
        return Map.of("message", "Incident archived.");
    }
}
