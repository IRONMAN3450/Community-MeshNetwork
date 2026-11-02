package org.example.mesh2.rescue;

import jakarta.validation.Valid;
import java.util.List;
import org.example.mesh2.rescue.RescueDtos.AssignRequest;
import org.example.mesh2.rescue.RescueDtos.AssignmentResponse;
import org.example.mesh2.rescue.RescueDtos.CreateTeamRequest;
import org.example.mesh2.rescue.RescueDtos.TeamResponse;
import org.example.mesh2.rescue.RescueDtos.UpdateAssignmentStatusRequest;
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
@RequestMapping("/api/rescue")
public class RescueController {
    private final RescueService rescueService;

    public RescueController(RescueService rescueService) {
        this.rescueService = rescueService;
    }

    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rescueService.createTeam(request));
    }

    @GetMapping("/teams")
    public List<TeamResponse> listTeams() {
        return rescueService.listTeams();
    }

    @PostMapping("/assign")
    public AssignmentResponse assign(@Valid @RequestBody AssignRequest request) {
        return rescueService.assign(request);
    }

    @GetMapping("/assignments")
    public List<AssignmentResponse> listAssignments() {
        return rescueService.listAssignments();
    }

    @PutMapping("/assignments/{id}/status")
    public AssignmentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateAssignmentStatusRequest request) {
        return rescueService.updateAssignmentStatus(id, request.status());
    }

    @PostMapping("/assignments/{id}/advance")
    public AssignmentResponse advance(@PathVariable Long id) {
        return rescueService.advanceAssignment(id);
    }
}
