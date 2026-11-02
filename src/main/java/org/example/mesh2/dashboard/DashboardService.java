package org.example.mesh2.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.mesh2.account.UserAccountRepository;
import org.example.mesh2.account.UserRole;
import org.example.mesh2.incident.IncidentDtos.IncidentResponse;
import org.example.mesh2.incident.IncidentRepository;
import org.example.mesh2.incident.IncidentService;
import org.example.mesh2.mesh.MeshService;
import org.example.mesh2.node.NetworkNodeRepository;
import org.example.mesh2.node.NodeDtos.NodeResponse;
import org.example.mesh2.rescue.RescueDtos.TeamResponse;
import org.example.mesh2.rescue.RescueTeamRepository;
import org.example.mesh2.sos.SosAlertRepository;
import org.example.mesh2.sos.SosDtos.SosResponse;
import org.example.mesh2.sos.SosStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final SosAlertRepository sosAlerts;
    private final IncidentRepository incidents;
    private final IncidentService incidentService;
    private final RescueTeamRepository teams;
    private final NetworkNodeRepository nodes;
    private final UserAccountRepository accounts;
    private final MeshService meshService;

    public DashboardService(SosAlertRepository sosAlerts, IncidentRepository incidents, IncidentService incidentService,
                            RescueTeamRepository teams, NetworkNodeRepository nodes,
                            UserAccountRepository accounts, MeshService meshService) {
        this.sosAlerts = sosAlerts;
        this.incidents = incidents;
        this.incidentService = incidentService;
        this.teams = teams;
        this.nodes = nodes;
        this.accounts = accounts;
        this.meshService = meshService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<SosStatus> closedSosStatuses = List.of(SosStatus.RESOLVED, SosStatus.CANCELLED);
        long activeSos = sosAlerts.countByStatusNotIn(closedSosStatuses);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeIncidents", incidents.countByStatusIgnoreCaseNot("ARCHIVED") - incidents.countByStatusIgnoreCase("RESOLVED"));
        payload.put("activeSos", Math.max(activeSos, 0));
        payload.put("resolvedIncidents", incidents.countByStatusIgnoreCase("RESOLVED"));
        payload.put("teamsDeployed", teams.countByStatusIgnoreCase("DEPLOYED"));
        payload.put("teamsAvailable", teams.countByStatusIgnoreCase("AVAILABLE"));
        payload.put("activeNodes", nodes.countByStatusIgnoreCase("ONLINE"));
        payload.put("totalNodes", nodes.count());
        payload.put("connectedMeshClients", meshService.connectedClients());
        payload.put("citizens", accounts.countByRole(UserRole.CITIZEN));
        payload.put("responders", accounts.countByRole(UserRole.RESPONDER) + accounts.countByRole(UserRole.ADMIN));
        payload.put("received", sosAlerts.countByStatus(SosStatus.RECEIVED));
        payload.put("assigned", sosAlerts.countByStatus(SosStatus.ASSIGNED));
        payload.put("inProgress", sosAlerts.countByStatus(SosStatus.IN_PROGRESS));
        payload.put("resolved", sosAlerts.countByStatus(SosStatus.RESOLVED));
        payload.put("recentIncidents", incidentService.listRecentActive());
        payload.put("recentSosAlerts", sosAlerts.findTop10ByStatusNotInOrderByCreatedAtDesc(closedSosStatuses)
                .stream().map(SosResponse::from).toList());
        return payload;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mapData() {
        return Map.of(
                "sosAlerts", sosAlerts.findByOrderByCreatedAtDesc().stream().map(SosResponse::from).toList(),
                "incidents", incidents.findByOrderByCreatedAtDesc().stream().map(IncidentResponse::from).toList(),
                "teams", teams.findByOrderByNameAsc().stream().map(TeamResponse::from).toList(),
                "nodes", nodes.findByOrderByCreatedAtDesc().stream().map(NodeResponse::from).toList()
        );
    }
}
