package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "friendship")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One friend
    @ManyToOne(optional = false)
    @JoinColumn(name = "user1_id")
    private User user1;

    // The other friend
    @ManyToOne(optional = false)
    @JoinColumn(name = "user2_id")
    private User user2;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters and setters

    public Long getId() {
        return id;
    }
    public User getUser1() {
        return user1;
    }
    public void setUser1(User user1) {
        this.user1 = user1;
    }
    public User getUser2() {
        return user2;
    }
    public void setUser2(User user2) {
        this.user2 = user2;
    }
    public Instant getCreatedAt() {
        return createdAt;
    }
}
