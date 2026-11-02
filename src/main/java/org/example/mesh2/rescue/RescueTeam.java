package org.example.mesh2.rescue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rescue_teams")
public class RescueTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String specialty;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    private Double latitude;
    private Double longitude;
    private int memberCount = 4;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected RescueTeam() {}

    public RescueTeam(String name, String specialty, Double latitude, Double longitude, int memberCount) {
        this.name = name;
        this.specialty = specialty;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memberCount = memberCount;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getStatus() { return status; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public int getMemberCount() { return memberCount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public void setStatus(String status) { this.status = status; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
