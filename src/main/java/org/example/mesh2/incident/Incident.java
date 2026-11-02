package org.example.mesh2.incident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "incidents")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String sector;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String severity = "HIGH";

    @Column(nullable = false)
    private String status = "ACTIVE";

    private Double latitude;
    private Double longitude;
    private long activeSosCount;
    private long responseTeamCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected Incident() {}

    public Incident(String title, String type, String sector, String description, String severity,
                    Double latitude, Double longitude) {
        this.title = title;
        this.type = type;
        this.sector = sector;
        this.description = description;
        this.severity = severity == null || severity.isBlank() ? "HIGH" : severity.toUpperCase(java.util.Locale.ROOT);
        this.latitude = latitude;
        this.longitude = longitude;
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
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public String getSector() { return sector; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public String getStatus() { return status; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public long getActiveSosCount() { return activeSosCount; }
    public long getResponseTeamCount() { return responseTeamCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setType(String type) { this.type = type; }
    public void setSector(String sector) { this.sector = sector; }
    public void setDescription(String description) { this.description = description; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setStatus(String status) { this.status = status; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setActiveSosCount(long activeSosCount) { this.activeSosCount = activeSosCount; }
    public void setResponseTeamCount(long responseTeamCount) { this.responseTeamCount = responseTeamCount; }
}
