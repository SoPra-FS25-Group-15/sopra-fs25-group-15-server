package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class StartGameMessage {
    private int roundCount;
    private int roundTime;

    public StartGameMessage() { }

    public StartGameMessage(int roundCount, int roundTime) {
        this.roundCount = roundCount;
        this.roundTime  = roundTime;
    }

    public int getRoundCount() {
        return roundCount;
    }

    public void setRoundCount(int roundCount) {
        this.roundCount = roundCount;
    }

    public int getRoundTime() {
        return roundTime;
    }

    public void setRoundTime(int roundTime) {
        this.roundTime = roundTime;
    }
}
