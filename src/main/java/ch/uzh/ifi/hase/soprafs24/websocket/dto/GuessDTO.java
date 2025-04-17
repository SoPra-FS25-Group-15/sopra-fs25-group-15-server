package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GuessDTO {
    private int roundNumber;
    private int userGuess;

    public GuessDTO() {}
    public GuessDTO(int roundNumber, int userGuess) {
        this.roundNumber = roundNumber;
        this.userGuess = userGuess;
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public int getUserGuess() { return userGuess; }
    public void setUserGuess(int userGuess) { this.userGuess = userGuess; }
}
