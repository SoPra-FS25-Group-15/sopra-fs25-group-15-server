package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GuessReceivedDTO {
    private int roundNumber;
    private int yourGuess;

    public GuessReceivedDTO() {}
    public GuessReceivedDTO(int roundNumber, int yourGuess) {
        this.roundNumber = roundNumber;
        this.yourGuess = yourGuess;
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public int getYourGuess() { return yourGuess; }
    public void setYourGuess(int yourGuess) { this.yourGuess = yourGuess; }
}
