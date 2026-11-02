package org.example.mesh2.rescue;

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
import org.example.mesh2.sos.SosAlert;
import org.example.mesh2.sos.SosStatus;

@Entity
@Table(name = "rescue_assignments")
public class RescueAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sos_alert_id", nullable = false)
    private Long sosAlertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sos_alert_id", insertable = false, updatable = false)
    private SosAlert sosAlert;

    @Column(nullable = false)
    private String sosPublicCode;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    private RescueTeam team;

    @Column(nullable = false)
    private String teamName;

    private String category;
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SosStatus status = SosStatus.ASSIGNED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    protected RescueAssignment() {}

    public RescueAssignment(Long sosAlertId, String sosPublicCode, Long teamId, String teamName,
                            String category, String notes, SosStatus status) {
        this.sosAlertId = sosAlertId;
        this.sosPublicCode = sosPublicCode;
        this.teamId = teamId;
        this.teamName = teamName;
        this.category = category;
        this.notes = notes;
        this.status = status;
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
    public Long getSosAlertId() { return sosAlertId; }
    public String getSosPublicCode() { return sosPublicCode; }
    public Long getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public String getCategory() { return category; }
    public String getNotes() { return notes; }
    public SosStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(SosStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public void setTeamName(String teamName) { this.teamName = teamName; }
}
