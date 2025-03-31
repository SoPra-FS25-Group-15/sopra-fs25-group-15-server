package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friend_request")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who sent the request
    @ManyToOne(optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    // The user who is to receive the request
    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.status = FriendRequestStatus.PENDING;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }
    public User getSender() {
        return sender;
    }
    public void setSender(User sender) {
        this.sender = sender;
    }
    public User getRecipient() {
        return recipient;
    }
    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }
    public FriendRequestStatus getStatus() {
        return status;
    }
    public void setStatus(FriendRequestStatus status) {
        this.status = status;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}
