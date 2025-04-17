package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.List;

public class RoundResultsMessage {
    private final String type    = "ROUND_RESULTS";
    private final boolean gameEnd;
    private final List<PlayerResultDTO> results;

    public RoundResultsMessage(boolean gameEnd, List<PlayerResultDTO> results) {
        this.gameEnd = gameEnd;
        this.results = results;
    }

    public String getType() {
        return type;
    }

    public boolean isGameEnd() {
        return gameEnd;
    }

    public List<PlayerResultDTO> getResults() {
        return results;
    }
}
