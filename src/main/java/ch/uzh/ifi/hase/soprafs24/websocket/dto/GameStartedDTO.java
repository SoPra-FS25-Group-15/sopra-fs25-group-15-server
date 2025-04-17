package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GameStartedDTO {
    private int roundCount;
    private int roundTime;

    public GameStartedDTO() {}
    public GameStartedDTO(int roundCount, int roundTime) {
        this.roundCount = roundCount;
        this.roundTime = roundTime;
    }

    public int getRoundCount() { return roundCount; }
    public void setRoundCount(int roundCount) { this.roundCount = roundCount; }
    public int getRoundTime() { return roundTime; }
    public void setRoundTime(int roundTime) { this.roundTime = roundTime; }
}
