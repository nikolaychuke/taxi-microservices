package org.example.taxi.notification.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "notification_tasks", indexes = @Index(name = "idx_notification_status", columnList = "status"))
public class NotificationTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "trip_id")
    private Long tripId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", foreignKey = @ForeignKey(name = "fk_notification_task_trip"), insertable = false, updatable = false)
    private TripRef tripRef;
    @Enumerated(EnumType.STRING)
    private RecipientType recipientType;
    private Long recipientId;
    private String message;
    @Enumerated(EnumType.STRING)
    private NotificationTaskStatus status = NotificationTaskStatus.PENDING;
    private Integer attempts = 0;
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }
    public RecipientType getRecipientType() { return recipientType; }
    public void setRecipientType(RecipientType recipientType) { this.recipientType = recipientType; }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public NotificationTaskStatus getStatus() { return status; }
    public void setStatus(NotificationTaskStatus status) { this.status = status; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public Instant getCreatedAt() { return createdAt; }
}
