package org.example.mesh2.sos;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.common.ApiException;
import org.example.mesh2.common.RequestContext;
import org.example.mesh2.incident.IncidentRepository;
import org.example.mesh2.sos.SosDtos.CreateSosRequest;
import org.example.mesh2.sos.SosDtos.SosResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SosService {
    private final SosAlertRepository sosAlerts;
    private final IncidentRepository incidents;

    public SosService(SosAlertRepository sosAlerts, IncidentRepository incidents) {
        this.sosAlerts = sosAlerts;
        this.incidents = incidents;
    }

    @Transactional
    public SosResponse create(CreateSosRequest request) {
        UserAccount reporter = RequestContext.requireCurrentUser();
        validateSeverity(request.severity());
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude and longitude must be provided together.");
        }
        Long incidentId = request.incidentId();
        if (incidentId != null) {
            var incident = incidents.findById(incidentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Linked incident was not found."));
            if ("RESOLVED".equalsIgnoreCase(incident.getStatus()) || "ARCHIVED".equalsIgnoreCase(incident.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "SOS alerts cannot be linked to a closed incident.");
            }
        }

        SosAlert alert = new SosAlert(
                nextPublicCode(),
                reporter.getId(),
                reporter.getFullName(),
                request.category().trim(),
                blankToNull(request.description()),
                request.latitude() != null ? request.latitude() : reporter.getLatitude(),
                request.longitude() != null ? request.longitude() : reporter.getLongitude(),
                blankToNull(request.locationLabel()),
                request.severity()
        );
        alert.setIncidentId(incidentId);
        SosAlert saved = sosAlerts.save(alert);
        if (incidentId != null) {
            incidents.findById(incidentId).ifPresent(incident -> {
                incident.setActiveSosCount(sosAlerts.countByIncidentIdAndStatusNotIn(
                        incident.getId(), List.of(SosStatus.RESOLVED, SosStatus.CANCELLED)));
                incidents.save(incident);
            });
        }
        return SosResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SosResponse> listAll() {
        return sosAlerts.findByOrderByCreatedAtDesc().stream().map(SosResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SosResponse> listMine() {
        UserAccount user = RequestContext.requireCurrentUser();
        return sosAlerts.findByReporterIdOrderByCreatedAtDesc(user.getId()).stream().map(SosResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SosResponse> listActive() {
        return sosAlerts.findByStatusNotOrderByCreatedAtDesc(SosStatus.RESOLVED).stream()
                .filter(alert -> alert.getStatus() != SosStatus.CANCELLED)
                .map(SosResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SosResponse getById(Long id) {
        return SosResponse.from(requireAlert(id));
    }

    @Transactional
    public SosResponse updateStatus(Long id, String statusValue) {
        SosAlert alert = requireAlert(id);
        SosStatus status;
        try {
            status = SosStatus.valueOf(statusValue.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid SOS status.");
        }
        alert.setStatus(status);
        return SosResponse.from(sosAlerts.save(alert));
    }

    @Transactional
    public SosResponse advanceStatus(Long id) {
        SosAlert alert = requireAlert(id);
        alert.setStatus(alert.getStatus().next());
        return SosResponse.from(sosAlerts.save(alert));
    }

    private SosAlert requireAlert(Long id) {
        return sosAlerts.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS alert not found."));
    }

    private String nextPublicCode() {
        return "CM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private void validateSeverity(String severityValue) {
        if (severityValue == null || severityValue.isBlank()) return;
        String severity = severityValue.trim().toUpperCase(Locale.ROOT);
        if (!List.of("CRITICAL", "HIGH", "MEDIUM", "LOW").contains(severity)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid SOS severity.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
