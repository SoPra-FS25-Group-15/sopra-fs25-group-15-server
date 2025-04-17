package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GuessRejectedDTO {
    private int roundNumber;
    private String reason;

    public GuessRejectedDTO() {}
    public GuessRejectedDTO(int roundNumber, String reason) {
        this.roundNumber = roundNumber;
        this.reason     = reason;
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
