package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GameWinnerBroadcast {
    private final String type = "GAME_WINNER";
    private final String winnerUsername;

    public GameWinnerBroadcast(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }

    public String getType() {
        return type;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }
}
