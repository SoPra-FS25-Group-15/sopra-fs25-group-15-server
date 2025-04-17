package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.List;

public class PlayerResultDTO {
    private final String username;
    private final int totalScore;
    private final List<GuessResultDTO> guesses;

    public PlayerResultDTO(String username, int totalScore, List<GuessResultDTO> guesses) {
        this.username   = username;
        this.totalScore = totalScore;
        this.guesses    = guesses;
    }

    public String getUsername() {
        return username;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public List<GuessResultDTO> getGuesses() {
        return guesses;
    }
}
