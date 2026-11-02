package org.example.mesh2.sos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.example.mesh2.account.UserAccount;
import org.example.mesh2.incident.Incident;
import org.example.mesh2.rescue.RescueTeam;

@Entity
@Table(name = "sos_alerts")
public class SosAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String publicCode;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", insertable = false, updatable = false)
    private UserAccount reporter;

    @Column(nullable = false)
    private String reporterName;

    @Column(nullable = false)
    private String category;

    @Column(length = 1000)
    private String description;

    private Double latitude;
    private Double longitude;
    private String locationLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SosStatus status = SosStatus.RECEIVED;

    @Column(nullable = false)
    private String severity = "HIGH";

    @Column(name = "incident_id")
    private Long incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", insertable = false, updatable = false)
    private Incident incident;

    @Column(name = "assigned_team_id")
    private Long assignedTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id", insertable = false, updatable = false)
    private RescueTeam assignedTeam;

    private String assignedTeamName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;
    private Instant resolvedAt;

    protected SosAlert() {}

    public SosAlert(String publicCode, Long reporterId, String reporterName, String category, String description,
                    Double latitude, Double longitude, String locationLabel, String severity) {
        this.publicCode = publicCode;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.category = category;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationLabel = locationLabel;
        this.severity = severity == null || severity.isBlank() ? "HIGH" : severity.toUpperCase(java.util.Locale.ROOT);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (status == SosStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = updatedAt;
        }
    }

    public Long getId() { return id; }
    public String getPublicCode() { return publicCode; }
    public Long getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getLocationLabel() { return locationLabel; }
    public SosStatus getStatus() { return status; }
    public String getSeverity() { return severity; }
    public Long getIncidentId() { return incidentId; }
    public Long getAssignedTeamId() { return assignedTeamId; }
    public String getAssignedTeamName() { return assignedTeamName; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void setStatus(SosStatus status) { this.status = status; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public void setAssignedTeamId(Long assignedTeamId) { this.assignedTeamId = assignedTeamId; }
    public void setAssignedTeamName(String assignedTeamName) { this.assignedTeamName = assignedTeamName; }
    public void setDescription(String description) { this.description = description; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setLocationLabel(String locationLabel) { this.locationLabel = locationLabel; }
}
