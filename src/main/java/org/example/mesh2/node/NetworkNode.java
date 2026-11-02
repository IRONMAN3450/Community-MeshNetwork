package org.example.mesh2.node;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "network_nodes")
public class NetworkNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nodeCode;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String nodeType;

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", insertable = false, updatable = false)
    private UserAccount owner;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String status = "ONLINE";

    private Instant lastSeenAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NetworkNode() {}

    public NetworkNode(String nodeCode, String displayName, String nodeType, Long ownerUserId,
                       Double latitude, Double longitude) {
        this.nodeCode = nodeCode;
        this.displayName = displayName;
        this.nodeType = nodeType;
        this.ownerUserId = ownerUserId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastSeenAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (lastSeenAt == null) {
            lastSeenAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        lastSeenAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getNodeCode() { return nodeCode; }
    public String getDisplayName() { return displayName; }
    public String getNodeType() { return nodeType; }
    public Long getOwnerUserId() { return ownerUserId; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getStatus() { return status; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setStatus(String status) { this.status = status; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
