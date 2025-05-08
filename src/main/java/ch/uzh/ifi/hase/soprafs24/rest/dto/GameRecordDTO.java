// src/main/java/ch/uzh/ifi/hase/soprafs24/rest/dto/GameRecordDTO.java
package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameRecordDTO {
    private String winner;
    private List<String> players;
    private int roundsPlayed;
    private int roundCardStartAmount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // getters & setters
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }

    public List<String> getPlayers() { return players; }
    public void setPlayers(List<String> players) { this.players = players; }

    public int getRoundsPlayed() { return roundsPlayed; }
    public void setRoundsPlayed(int roundsPlayed) { this.roundsPlayed = roundsPlayed; }

    public int getRoundCardStartAmount() { return roundCardStartAmount; }
    public void setRoundCardStartAmount(int roundCardStartAmount) { this.roundCardStartAmount = roundCardStartAmount; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
