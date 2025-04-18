package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class GuessResultDTO {
    private final Integer guess;
    private final Integer diff;

    public GuessResultDTO(Integer guess, Integer diff) {
        this.guess = guess;
        this.diff  = diff;
    }

    public Integer getGuess() {
        return guess;
    }

    public Integer getDiff() {
        return diff;
    }
}
