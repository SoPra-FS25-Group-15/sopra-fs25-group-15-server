package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GuessMessage {
    private int guess;

    public GuessMessage() { }

    public GuessMessage(int guess) {
        this.guess = guess;
    }

    public int getGuess() {
        return guess;
    }

    public void setGuess(int guess) {
        this.guess = guess;
    }
}
