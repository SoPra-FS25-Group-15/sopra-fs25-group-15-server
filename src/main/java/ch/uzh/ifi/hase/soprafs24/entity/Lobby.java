package ch.uzh.ifi.hase.soprafs24.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;

@Entity
@Table(name = "lobby")
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mode;

    // Indicates if the lobby is private (true) or public (false)
    @Column(nullable = false)
    private boolean isPrivate;

    // Only for private lobbies: auto‑generated lobby code.
    private String lobbyCode;

    @Column(nullable = false)
    private Integer maxPlayersPerTeam;
    
    // Maximum number of players for solo mode
    @Column
    private Integer maxPlayers;

    // Stores the round cards (hints) for the lobby.
    @ElementCollection
    @CollectionTable(name = "lobby_hints", joinColumns = @JoinColumn(name = "lobby_id"))
    @Column(name = "round_card")
    private List<String> hintsEnabled = new ArrayList<>();

    // Reference to the host of the lobby.
    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // Dynamic teams mapping (not persisted)
    @Transient
    private Map<String, List<User>> teams = new HashMap<>();
    
    // Players list for solo mode
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "lobby_players",
        joinColumns = @JoinColumn(name = "lobby_id"),
        inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    private List<User> players = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String status;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        status = LobbyConstants.LOBBY_STATUS_WAITING;
        
        if (this.mode == null) {
            this.mode = LobbyConstants.MODE_SOLO;
        }
        
        if (this.mode.equals(LobbyConstants.MODE_SOLO)) {
            this.maxPlayersPerTeam = 1;
            if (this.maxPlayers == null) {
                this.maxPlayers = 8;
            }
        } else {
            this.maxPlayersPerTeam = 2;
            if (this.maxPlayers == null) {
                this.maxPlayers = 8;
            }
        }
        
        // Generate a lobby code only for private lobbies (unranked)
        if (this.isPrivate) {
            lobbyCode = generateLobbyCode();
        }
        
        // Initialize with default hints (empty list)
        if (this.hintsEnabled == null) {
            this.hintsEnabled = new ArrayList<>();
        }
    }

    private String generateLobbyCode() {
        // Creates an 5-character alphanumeric code.
        return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    // Standard getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
    
    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }
    
    public Integer getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }
    
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    
    public List<String> getHintsEnabled() { return hintsEnabled; }
    public void setHintsEnabled(List<String> hintsEnabled) { this.hintsEnabled = hintsEnabled; }
    
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
    
    // Teams mapping is transient (not persisted)
    public Map<String, List<User>> getTeams() { return teams; }
    public void setTeams(Map<String, List<User>> teams) { this.teams = teams; }
    
    // Players list for solo mode
    public List<User> getPlayers() { return players; }
    public void setPlayers(List<User> players) { this.players = players; }
    
    public Instant getCreatedAt() { return createdAt; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}