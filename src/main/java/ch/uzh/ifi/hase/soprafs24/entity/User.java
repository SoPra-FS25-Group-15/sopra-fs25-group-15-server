package ch.uzh.ifi.hase.soprafs24.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

@Entity
@Table(name = "USER")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    // Authentication fields
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // A session token to be issued upon registration/login
    @Column(unique = true)
    private String token;

    // Optionally, a status field
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    // Creation timestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // One-to-one relationship with UserProfile
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private UserProfile profile;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }

    // Getters and setters

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
    // ...
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public UserStatus getStatus() {
        return status;
    }
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserProfile getProfile() {
        return profile;
    }
    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public void generateToken() {
        this.token = UUID.randomUUID().toString();
    }
    public void setUsername(String testuser) {
    }
}
