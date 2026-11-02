package org.example.mesh2.mesh;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import org.example.mesh2.account.UserAccount;

@Entity
@Table(name = "mesh_messages")
public class MeshMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id")
    private Long senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private UserAccount sender;

    @Column(nullable = false)
    private String senderName;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String deliveryStatus = "DELIVERED";

    private int hopCount = 1;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MeshMessage() {}

    public MeshMessage(Long senderId, String senderName, String content, String deliveryStatus, int hopCount) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.deliveryStatus = deliveryStatus;
        this.hopCount = hopCount;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public int getHopCount() { return hopCount; }
    public Instant getCreatedAt() { return createdAt; }
}
