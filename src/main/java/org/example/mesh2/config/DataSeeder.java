package org.example.mesh2.config;

import org.example.mesh2.account.UserAccount;
import org.example.mesh2.account.UserAccountRepository;
import org.example.mesh2.account.UserRole;
import org.example.mesh2.incident.Incident;
import org.example.mesh2.incident.IncidentRepository;
import org.example.mesh2.mesh.MeshMessage;
import org.example.mesh2.mesh.MeshMessageRepository;
import org.example.mesh2.node.NetworkNode;
import org.example.mesh2.node.NetworkNodeRepository;
import org.example.mesh2.rescue.RescueAssignment;
import org.example.mesh2.rescue.RescueAssignmentRepository;
import org.example.mesh2.rescue.RescueTeam;
import org.example.mesh2.rescue.RescueTeamRepository;
import org.example.mesh2.sos.SosAlert;
import org.example.mesh2.sos.SosAlertRepository;
import org.example.mesh2.sos.SosStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {
    private final boolean seedDemoData;
    private final UserAccountRepository accounts;
    private final IncidentRepository incidents;
    private final SosAlertRepository sosAlerts;
    private final RescueTeamRepository teams;
    private final RescueAssignmentRepository assignments;
    private final NetworkNodeRepository nodes;
    private final MeshMessageRepository messages;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataSeeder(
            @Value("${community.mesh.seed-demo-data:false}") boolean seedDemoData,
            UserAccountRepository accounts,
            IncidentRepository incidents,
            SosAlertRepository sosAlerts,
            RescueTeamRepository teams,
            RescueAssignmentRepository assignments,
            NetworkNodeRepository nodes,
            MeshMessageRepository messages
    ) {
        this.seedDemoData = seedDemoData;
        this.accounts = accounts;
        this.incidents = incidents;
        this.sosAlerts = sosAlerts;
        this.teams = teams;
        this.assignments = assignments;
        this.nodes = nodes;
        this.messages = messages;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemoData || accounts.count() > 0) {
            return;
        }

        UserAccount admin = accounts.save(buildUser("Asha", "Coordinator", "admin@mesh.local", "9000000001", "Admin", UserRole.ADMIN, 28.6139, 77.2090));
        UserAccount responder = accounts.save(buildUser("Ravi", "Responder", "responder@mesh.local", "9000000002", "Responder", UserRole.RESPONDER, 28.6200, 77.2100));
        UserAccount citizen = accounts.save(buildUser("Neha", "Citizen", "citizen@mesh.local", "9000000003", "Citizen", UserRole.CITIZEN, 28.6120, 77.2200));
        citizen.setEmergencyContactName("Anil Citizen");
        citizen.setEmergencyContactPhone("9000000099");
        accounts.save(citizen);

        Incident flood = incidents.save(new Incident("Flood — Sector A", "FLOOD", "Sector A — Riverside", "Water levels rising rapidly near the riverside.", "CRITICAL", 28.6145, 77.2085));
        Incident quake = incidents.save(new Incident("Earthquake — Sector B", "EARTHQUAKE", "Sector B — Central", "Building damage reported in central blocks.", "CRITICAL", 28.6180, 77.2150));
        Incident fire = incidents.save(new Incident("Fire — Sector C", "FIRE", "Sector C — Industrial", "Warehouse fire spreading toward storage units.", "HIGH", 28.6100, 77.2250));
        Incident rain = incidents.save(new Incident("Heavy Rain — Sector D", "CYCLONE", "Sector D — Coastal", "Strong winds and rainfall under monitoring.", "MEDIUM", 28.6050, 77.2300));

        RescueTeam alpha = teams.save(new RescueTeam("Rescue Team Alpha", "Search & Rescue", 28.6160, 77.2120, 6));
        RescueTeam medical = teams.save(new RescueTeam("Medical Team 02", "Medical", 28.6190, 77.2180, 4));
        RescueTeam fireTeam = teams.save(new RescueTeam("Fire Response Unit", "Firefighting", 28.6110, 77.2220, 5));
        RescueTeam evacuate = teams.save(new RescueTeam("Evacuation Team", "Evacuation", 28.6070, 77.2280, 8));
        alpha.setStatus("DEPLOYED");
        medical.setStatus("DEPLOYED");
        fireTeam.setStatus("AVAILABLE");
        evacuate.setStatus("AVAILABLE");
        teams.save(alpha);
        teams.save(medical);
        teams.save(fireTeam);
        teams.save(evacuate);

        SosAlert trapped = sosAlerts.save(new SosAlert("CM-1042", citizen.getId(), citizen.getFullName(), "Trapped",
                "Person trapped under debris near Sector B market.", 28.6175, 77.2140, "Sector B Market", "CRITICAL"));
        trapped.setIncidentId(quake.getId());
        trapped.setAssignedTeamId(alpha.getId());
        trapped.setAssignedTeamName(alpha.getName());
        trapped.setStatus(SosStatus.IN_PROGRESS);
        sosAlerts.save(trapped);

        SosAlert medicalSos = sosAlerts.save(new SosAlert("CM-1041", citizen.getId(), citizen.getFullName(), "Medical",
                "Urgent medical help needed in Sector A shelter.", 28.6130, 77.2095, "Sector A Shelter", "HIGH"));
        medicalSos.setIncidentId(flood.getId());
        medicalSos.setAssignedTeamId(medical.getId());
        medicalSos.setAssignedTeamName(medical.getName());
        medicalSos.setStatus(SosStatus.ASSIGNED);
        sosAlerts.save(medicalSos);

        SosAlert evacuateSos = sosAlerts.save(new SosAlert("CM-1038", citizen.getId(), citizen.getFullName(), "Evacuation",
                "Family requesting evacuation assistance.", 28.6065, 77.2290, "Sector D Coast Road", "MEDIUM"));
        evacuateSos.setIncidentId(rain.getId());
        evacuateSos.setAssignedTeamId(evacuate.getId());
        evacuateSos.setAssignedTeamName(evacuate.getName());
        evacuateSos.setStatus(SosStatus.RESOLVED);
        sosAlerts.save(evacuateSos);

        SosAlert fireSos = sosAlerts.save(new SosAlert("CM-1043", admin.getId(), admin.getFullName(), "Fire",
                "Smoke reported near industrial storage.", 28.6105, 77.2245, "Sector C Warehouse", "HIGH"));
        fireSos.setIncidentId(fire.getId());
        fireSos.setStatus(SosStatus.RECEIVED);
        sosAlerts.save(fireSos);

        flood.setActiveSosCount(1);
        flood.setResponseTeamCount(1);
        quake.setActiveSosCount(1);
        quake.setResponseTeamCount(1);
        fire.setActiveSosCount(1);
        rain.setActiveSosCount(0);
        rain.setResponseTeamCount(1);
        incidents.save(flood);
        incidents.save(quake);
        incidents.save(fire);
        incidents.save(rain);

        assignments.save(new RescueAssignment(trapped.getId(), trapped.getPublicCode(), alpha.getId(), alpha.getName(),
                trapped.getCategory(), "Primary search unit", SosStatus.IN_PROGRESS));
        assignments.save(new RescueAssignment(medicalSos.getId(), medicalSos.getPublicCode(), medical.getId(), medical.getName(),
                medicalSos.getCategory(), "Stabilize and transport", SosStatus.ASSIGNED));
        assignments.save(new RescueAssignment(evacuateSos.getId(), evacuateSos.getPublicCode(), evacuate.getId(), evacuate.getName(),
                evacuateSos.getCategory(), "Completed coastal evacuation", SosStatus.RESOLVED));

        nodes.save(new NetworkNode("NODE-GATEWAY", "Community Gateway", "GATEWAY", admin.getId(), 28.6139, 77.2090));
        nodes.save(new NetworkNode("NODE-ALPHA", "Responder Node Alpha", "RESPONDER", responder.getId(), 28.6160, 77.2120));
        nodes.save(new NetworkNode("NODE-SHELTER", "Sector A Shelter Node", "RELAY", citizen.getId(), 28.6130, 77.2095));
        nodes.save(new NetworkNode("NODE-COAST", "Coastal Relay", "RELAY", null, 28.6060, 77.2295));

        messages.save(new MeshMessage(citizen.getId(), citizen.getFullName(), "Water needed in Sector A.", "DELIVERED", 2));
        messages.save(new MeshMessage(responder.getId(), responder.getFullName(), "Emergency team notified.", "DELIVERED", 1));
        messages.save(new MeshMessage(admin.getId(), admin.getFullName(), "Keep shelter gate open for arrivals.", "DELIVERED", 3));
    }

    private UserAccount buildUser(String first, String last, String email, String phone, String userType,
                                  UserRole role, double lat, double lng) {
        UserAccount account = new UserAccount(first, last, email, phone, userType, passwordEncoder.encode("password123"));
        account.setRole(role);
        account.setLatitude(lat);
        account.setLongitude(lng);
        return account;
    }
}
