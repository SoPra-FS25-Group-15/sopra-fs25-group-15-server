package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;

/**
 * Internal Game representation
 * This class composes the internal representation of the game and defines how
 * the game is stored in the database
 */
@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String creatorUsername;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    @Column
    private Integer maxPlayers;

    @Column
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> players = new HashSet<>();

    @Column
    private Integer currentRound = 0;

    @Column
    private Long currentRoundWinnerId;

    @ManyToOne
    private User winner;

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }    public String getCreatorUsername() {
        return creatorUsername;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public Set<User> getPlayers() {
        return players;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public Long getCurrentRoundWinnerId() {
        return currentRoundWinnerId;
    }

    public User getWinner() {
        return winner;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setPlayers(Set<User> players) {
        this.players = players;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public void setCurrentRoundWinnerId(Long currentRoundWinnerId) {
        this.currentRoundWinnerId = currentRoundWinnerId;
    }

    public void setWinner(User winner) {
        this.winner = winner;
    }
}

