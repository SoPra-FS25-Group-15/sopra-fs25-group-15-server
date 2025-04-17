package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundStartMessage {
    private final String type = "ROUND_START";
    private final int round;

    public RoundStartMessage(int round) {
        this.round = round;
    }

    public String getType() {
        return type;
    }

    public int getRound() {
        return round;
    }
}
