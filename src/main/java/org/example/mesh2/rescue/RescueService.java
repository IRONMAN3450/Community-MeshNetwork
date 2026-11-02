package org.example.mesh2.rescue;

import java.util.List;
import java.util.Locale;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.incident.IncidentRepository;
import org.example.mesh2.rescue.RescueDtos.AssignRequest;
import org.example.mesh2.rescue.RescueDtos.AssignmentResponse;
import org.example.mesh2.rescue.RescueDtos.CreateTeamRequest;
import org.example.mesh2.rescue.RescueDtos.TeamResponse;
import org.example.mesh2.sos.SosAlert;
import org.example.mesh2.sos.SosAlertRepository;
import org.example.mesh2.sos.SosStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RescueService {
    private final RescueTeamRepository teams;
    private final RescueAssignmentRepository assignments;
    private final SosAlertRepository sosAlerts;
    private final IncidentRepository incidents;

    public RescueService(RescueTeamRepository teams, RescueAssignmentRepository assignments,
                         SosAlertRepository sosAlerts, IncidentRepository incidents) {
        this.teams = teams;
        this.assignments = assignments;
        this.sosAlerts = sosAlerts;
        this.incidents = incidents;
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        if (teams.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A team with this name already exists.");
        }
        RescueTeam team = new RescueTeam(
                request.name().trim(),
                request.specialty().trim(),
                request.latitude(),
                request.longitude(),
                request.memberCount() == null ? 4 : request.memberCount()
        );
        return TeamResponse.from(teams.save(team));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams() {
        return teams.findByOrderByNameAsc().stream().map(TeamResponse::from).toList();
    }

    @Transactional
    public AssignmentResponse assign(AssignRequest request) {
        SosAlert alert = sosAlerts.findById(request.sosAlertId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS alert not found."));
        RescueTeam team = teams.findById(request.teamId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Rescue team not found."));
        if (alert.getStatus() == SosStatus.RESOLVED || alert.getStatus() == SosStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Closed SOS alerts cannot be assigned.");
        }
        boolean activeAssignmentExists = assignments.findFirstBySosAlertIdOrderByCreatedAtDesc(alert.getId())
                .filter(existing -> existing.getStatus() != SosStatus.RESOLVED && existing.getStatus() != SosStatus.CANCELLED)
                .isPresent();
        if (activeAssignmentExists) {
            throw new ApiException(HttpStatus.CONFLICT, "This SOS alert already has an active rescue assignment.");
        }
        if ("DEPLOYED".equalsIgnoreCase(team.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This rescue team is already deployed.");
        }

        alert.setAssignedTeamId(team.getId());
        alert.setAssignedTeamName(team.getName());
        alert.setStatus(SosStatus.ASSIGNED);
        sosAlerts.save(alert);

        team.setStatus("DEPLOYED");
        teams.save(team);

        if (alert.getIncidentId() != null) {
            incidents.findById(alert.getIncidentId()).ifPresent(incident -> {
                incident.setResponseTeamCount(incident.getResponseTeamCount() + 1);
                incidents.save(incident);
            });
        }

        RescueAssignment assignment = assignments.save(new RescueAssignment(
                alert.getId(),
                alert.getPublicCode(),
                team.getId(),
                team.getName(),
                alert.getCategory(),
                blankToNull(request.notes()),
                SosStatus.ASSIGNED
        ));
        return toAssignmentResponse(assignment, alert.getSeverity());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments() {
        return assignments.findByOrderByCreatedAtDesc().stream()
                .map(assignment -> {
                    String severity = sosAlerts.findById(assignment.getSosAlertId())
                            .map(SosAlert::getSeverity)
                            .orElse("HIGH");
                    return toAssignmentResponse(assignment, severity);
                })
                .toList();
    }

    @Transactional
    public AssignmentResponse updateAssignmentStatus(Long id, String statusValue) {
        RescueAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found."));
        SosStatus status;
        try {
            status = SosStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid assignment status.");
        }
        SosStatus currentStatus = assignment.getStatus();
        if (status == currentStatus) {
            return toAssignmentResponse(assignment,
                    sosAlerts.findById(assignment.getSosAlertId()).map(SosAlert::getSeverity).orElse("HIGH"));
        }
        boolean canCancel = status == SosStatus.CANCELLED
                && (currentStatus == SosStatus.ASSIGNED || currentStatus == SosStatus.IN_PROGRESS);
        if (!canCancel && status != currentStatus.next()) {
            throw new ApiException(HttpStatus.CONFLICT, "Assignment status cannot skip or reverse workflow steps.");
        }
        assignment.setStatus(status);
        assignments.save(assignment);

        sosAlerts.findById(assignment.getSosAlertId()).ifPresent(alert -> {
            alert.setStatus(status);
            sosAlerts.save(alert);
        });

        if (status == SosStatus.RESOLVED || status == SosStatus.CANCELLED) {
            teams.findById(assignment.getTeamId()).ifPresent(team -> {
                team.setStatus("AVAILABLE");
                teams.save(team);
            });
        } else {
            teams.findById(assignment.getTeamId()).ifPresent(team -> {
                team.setStatus("DEPLOYED");
                teams.save(team);
            });
        }

        String severity = sosAlerts.findById(assignment.getSosAlertId())
                .map(SosAlert::getSeverity)
                .orElse("HIGH");
        return toAssignmentResponse(assignment, severity);
    }

    @Transactional
    public AssignmentResponse advanceAssignment(Long id) {
        RescueAssignment assignment = assignments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found."));
        return updateAssignmentStatus(id, assignment.getStatus().next().name());
    }

    private AssignmentResponse toAssignmentResponse(RescueAssignment assignment, String severity) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getSosAlertId(),
                assignment.getSosPublicCode(),
                assignment.getTeamId(),
                assignment.getTeamName(),
                assignment.getCategory(),
                assignment.getNotes(),
                assignment.getStatus().name(),
                severity,
                assignment.getCreatedAt() == null ? null : assignment.getCreatedAt().toString(),
                assignment.getUpdatedAt() == null ? null : assignment.getUpdatedAt().toString()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
