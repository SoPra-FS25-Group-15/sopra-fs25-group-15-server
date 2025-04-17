package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundWinnerBroadcast {
    private final String type = "ROUND_WINNER";
    private final String winnerUsername;
    private final int round;

    public RoundWinnerBroadcast(String winnerUsername, int round) {
        this.winnerUsername = winnerUsername;
        this.round = round;
    }

    public String getType() {
        return type;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public int getRound() {
        return round;
    }
}
