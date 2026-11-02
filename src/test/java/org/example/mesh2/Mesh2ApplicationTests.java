package org.example.mesh2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.example.mesh2.auth.AuthSession;
import org.example.mesh2.auth.AuthSessionRepository;
import org.example.mesh2.account.UserAccountRepository;
import org.example.mesh2.sos.SosAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Mesh2ApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private SosAlertRepository sosAlerts;

    @Autowired
    private AuthSessionRepository sessions;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndDashboardWorks() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSos").exists())
                .andExpect(jsonPath("$.activeNodes").exists())
                .andExpect(jsonPath("$.recentSosAlerts").isArray());
    }

    @Test
    void newlyCreatedSosAppearsInDashboardCountsAndActiveRequests() throws Exception {
        String citizenToken = login("citizen@mesh.local", "password123");
        JsonNode before = objectMapper.readTree(mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        MvcResult created = mockMvc.perform(post("/api/sos")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Medical\",\"description\":\"Dashboard refresh regression test\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long alertId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        JsonNode after = objectMapper.readTree(mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(before.path("activeSos").asLong() + 1, after.path("activeSos").asLong());
        assertEquals(alertId, after.path("recentSosAlerts").get(0).path("id").asLong());
        assertEquals("Dashboard refresh regression test",
                after.path("recentSosAlerts").get(0).path("description").asText());
    }

    @Test
    void meshStatusUsesHostIndependentWebSocketPath() throws Exception {
        mockMvc.perform(get("/api/mesh/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("/ws/mesh"))
                .andExpect(jsonPath("$.connectedClients").exists());
    }

    @Test
    void malformedJsonIsReportedAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is invalid."));
    }

    @Test
    void unknownApiEndpointReturnsStructuredNotFound() throws Exception {
        String citizenToken = login("citizen@mesh.local", "password123");
        mockMvc.perform(get("/api/not-a-real-endpoint").header("X-Auth-Token", citizenToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("API endpoint not found."));
    }

    @Test
    void expiredSessionIsRemovedAfterUnauthorizedResponse() throws Exception {
        var citizen = accounts.findByEmail("citizen@mesh.local").orElseThrow();
        String token = "expired-" + UUID.randomUUID();
        Instant now = Instant.now();
        sessions.saveAndFlush(new AuthSession(token, citizen.getId(), now.minus(2, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.DAYS)));

        mockMvc.perform(get("/api/accounts/me").header("X-Auth-Token", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expired. Please log in again."));

        assertTrue(sessions.findByToken(token).isEmpty(), "Expired auth sessions should be deleted.");
    }

    @Test
    void profileLocationCanBeSavedAndReturned() throws Exception {
        String citizenToken = login("citizen@mesh.local", "password123");

        mockMvc.perform(put("/api/accounts/me")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":28.6139,\"longitude\":77.2090}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139))
                .andExpect(jsonPath("$.longitude").value(77.2090));

        mockMvc.perform(get("/api/accounts/me")
                        .header("X-Auth-Token", citizenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139))
                .andExpect(jsonPath("$.longitude").value(77.2090));
    }

    @Test
    void signupCannotGrantPrivilegedRoleAndDeniedRequestDoesNotLeakIdentity() throws Exception {
        String email = "role-test-" + UUID.randomUUID() + "@mesh.local";
        MvcResult signup = mockMvc.perform(post("/api/accounts/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Role","lastName":"Test","email":"%s","phone":"9000000000",
                                 "userType":"ADMIN","password":"valid-password"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CITIZEN"))
                .andReturn();

        JsonNode account = objectMapper.readTree(signup.getResponse().getContentAsString());
        String token = account.path("token").asText();
        assertTrue(accounts.findByEmail(email).orElseThrow().getPasswordHash().startsWith("$2"));
        assertNotEquals("valid-password", accounts.findByEmail(email).orElseThrow().getPasswordHash());

        mockMvc.perform(post("/api/incidents")
                        .header("X-Auth-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Not allowed\",\"type\":\"FIRE\",\"sector\":\"Test\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/sos/1/advance")
                        .header("X-Auth-Token", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/mesh/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"anonymous after denied request\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderName").value("Anonymous Node"));
    }

    @Test
    void seededAdminCanManageIncidentsAndRescueTeams() throws Exception {
        String adminToken = login("admin@mesh.local", "password123");

        mockMvc.perform(post("/api/incidents")
                        .header("X-Auth-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Admin managed incident\",\"type\":\"FIRE\",\"sector\":\"Test sector\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/rescue/teams")
                        .header("X-Auth-Token", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Admin team " + UUID.randomUUID() + "\",\"specialty\":\"Response\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void loginCreateSosAndRejectInvalidCoordinates() throws Exception {
        String citizenToken = login("citizen@mesh.local", "password123");

        mockMvc.perform(post("/api/sos")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Medical","description":"Invalid location","latitude":91,"longitude":77}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/sos")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Medical","description":"Test","severity":"HIGH",
                                 "latitude":28.614,"longitude":77.21,"locationLabel":"Test Shelter"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicCode").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void incidentAndNodeUpdatesRejectInvalidCoordinatesAndNodeStatus() throws Exception {
        String responderToken = login("responder@mesh.local", "password123");
        String citizenToken = login("citizen@mesh.local", "password123");

        MvcResult incidentResult = mockMvc.perform(post("/api/incidents")
                        .header("X-Auth-Token", responderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Validation test\",\"type\":\"FIRE\",\"sector\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long incidentId = objectMapper.readTree(incidentResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(put("/api/incidents/" + incidentId)
                        .header("X-Auth-Token", responderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":91,\"longitude\":77}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/nodes")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Partial node location\",\"latitude\":28.6}"))
                .andExpect(status().isBadRequest());

        MvcResult nodeResult = mockMvc.perform(post("/api/nodes")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Heartbeat validation node\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long nodeId = objectMapper.readTree(nodeResult.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/nodes/" + nodeId + "/heartbeat")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":28.6}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/nodes/" + nodeId + "/heartbeat")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rescueAssignmentLifecycleAndDashboardCountersStayInSync() throws Exception {
        String citizenToken = login("citizen@mesh.local", "password123");
        String responderToken = login("responder@mesh.local", "password123");
        MvcResult created = mockMvc.perform(post("/api/sos")
                        .header("X-Auth-Token", citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Medical\",\"description\":\"Lifecycle test\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long alertId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        JsonNode teams = objectMapper.readTree(mockMvc.perform(get("/api/rescue/teams"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long teamId = -1;
        for (JsonNode team : teams) {
            if ("AVAILABLE".equals(team.path("status").asText())) {
                teamId = team.path("id").asLong();
                break;
            }
        }
        assertTrue(teamId > 0, "A seeded or newly registered rescue team should be available.");

        MvcResult assigned = mockMvc.perform(post("/api/rescue/assign")
                        .header("X-Auth-Token", responderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sosAlertId\":" + alertId + ",\"teamId\":" + teamId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andReturn();
        long assignmentId = objectMapper.readTree(assigned.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/advance")
                        .header("X-Auth-Token", responderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(put("/api/rescue/assignments/" + assignmentId + "/status")
                        .header("X-Auth-Token", responderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ASSIGNED\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/rescue/assignments/" + assignmentId + "/advance")
                        .header("X-Auth-Token", responderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        JsonNode dashboard = objectMapper.readTree(mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(sosAlerts.countByStatus(org.example.mesh2.sos.SosStatus.ASSIGNED),
                dashboard.path("assigned").asLong());
        assertEquals(sosAlerts.countByStatus(org.example.mesh2.sos.SosStatus.RESOLVED),
                dashboard.path("resolved").asLong());
    }

    @Test
    void databaseForeignKeysAreGeneratedForRelatedRecords() {
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS", Integer.class);
        assertTrue(foreignKeyCount != null && foreignKeyCount >= 8,
                "Expected database foreign keys for accounts, alerts, assignments, sessions, nodes, and messages.");
    }

    private String login(String email, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/accounts/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString()).path("token").asText();
    }
}
