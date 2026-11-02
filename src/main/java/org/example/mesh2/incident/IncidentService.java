package org.example.mesh2.incident;

import java.util.List;
import java.util.Locale;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.incident.IncidentDtos.CreateIncidentRequest;
import org.example.mesh2.incident.IncidentDtos.IncidentResponse;
import org.example.mesh2.incident.IncidentDtos.UpdateIncidentRequest;
import org.example.mesh2.sos.SosAlertRepository;
import org.example.mesh2.sos.SosStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private final IncidentRepository incidents;
    private final SosAlertRepository sosAlerts;

    public IncidentService(IncidentRepository incidents, SosAlertRepository sosAlerts) {
        this.incidents = incidents;
        this.sosAlerts = sosAlerts;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        validateSeverity(request.severity());
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        Incident incident = new Incident(
                request.title().trim(),
                request.type().trim().toUpperCase(Locale.ROOT),
                request.sector().trim(),
                blankToNull(request.description()),
                request.severity(),
                request.latitude(),
                request.longitude()
        );
        return IncidentResponse.from(incidents.save(incident));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listAll(String type) {
        List<Incident> results;
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type)) {
            results = incidents.findByOrderByCreatedAtDesc();
        } else {
            results = incidents.findByTypeIgnoreCaseOrderByCreatedAtDesc(type.trim());
        }
        return results.stream().map(this::refreshCounts).map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listRecentActive() {
        return incidents.findTop8ByStatusIgnoreCaseNotInOrderByCreatedAtDesc(List.of("RESOLVED", "ARCHIVED"))
                .stream().map(this::refreshCounts).map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id) {
        return IncidentResponse.from(refreshCounts(requireIncident(id)));
    }

    @Transactional
    public IncidentResponse update(Long id, UpdateIncidentRequest request) {
        Incident incident = requireIncident(id);
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        if (notBlank(request.title())) {
            incident.setTitle(request.title().trim());
        }
        if (notBlank(request.type())) {
            incident.setType(request.type().trim().toUpperCase(Locale.ROOT));
        }
        if (notBlank(request.sector())) {
            incident.setSector(request.sector().trim());
        }
        if (request.description() != null) {
            incident.setDescription(blankToNull(request.description()));
        }
        if (notBlank(request.severity())) {
            String severity = request.severity().trim().toUpperCase(Locale.ROOT);
            validateSeverity(severity);
            incident.setSeverity(severity);
        }
        if (notBlank(request.status())) {
            String status = request.status().trim().toUpperCase(Locale.ROOT);
            if (!List.of("ACTIVE", "IN_PROGRESS", "RESOLVED", "ARCHIVED").contains(status)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid incident status.");
            }
            incident.setStatus(status);
        }
        if (request.latitude() != null) {
            incident.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            incident.setLongitude(request.longitude());
        }
        return IncidentResponse.from(incidents.save(refreshCounts(incident)));
    }

    @Transactional
    public IncidentResponse archive(Long id) {
        Incident incident = requireIncident(id);
        incident.setStatus("ARCHIVED");
        return IncidentResponse.from(incidents.save(incident));
    }

    private Incident requireIncident(Long id) {
        return incidents.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Incident not found."));
    }

    private Incident refreshCounts(Incident incident) {
        long active = sosAlerts.countByIncidentIdAndStatusNotIn(
                incident.getId(), List.of(SosStatus.RESOLVED, SosStatus.CANCELLED));
        incident.setActiveSosCount(active);
        return incident;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void validateSeverity(String severityValue) {
        String severity = severityValue == null || severityValue.isBlank()
                ? "HIGH" : severityValue.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CRITICAL", "HIGH", "MEDIUM", "LOW").contains(severity)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid incident severity.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
