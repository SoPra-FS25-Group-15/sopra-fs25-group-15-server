package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundStartedDTO {
    private int roundNumber;
    private int roundTime;

    public RoundStartedDTO() {}
    public RoundStartedDTO(int roundNumber, int roundTime) {
        this.roundNumber = roundNumber;
        this.roundTime = roundTime;
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public int getRoundTime() { return roundTime; }
    public void setRoundTime(int roundTime) { this.roundTime = roundTime; }
}
