package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "GAME_RECORD")
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // link back to the user who this record belongs to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String winner;

    // one row per player in a separate table
    @ElementCollection
    @CollectionTable(
        name = "GAME_RECORD_PLAYERS",
        joinColumns = @JoinColumn(name = "game_record_id")
    )
    @Column(name = "player", nullable = false)
    private List<String> players;

    @Column(nullable = false)
    private int roundsPlayed;

    @Column(nullable = false)
    private int roundCardStartAmount;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    // --- getters & setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }

    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }

    public int getRoundCardStartAmount() { return roundCardStartAmount; }
    public void setRoundCardStartAmount(int roundCardStartAmount) {
        this.roundCardStartAmount = roundCardStartAmount;
    }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
