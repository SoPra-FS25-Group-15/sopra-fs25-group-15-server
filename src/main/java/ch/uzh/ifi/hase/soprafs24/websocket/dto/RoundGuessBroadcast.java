package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundGuessBroadcast {
    private final String type   = "ROUND_GUESS";
    private final String username;
    private final Integer guess;
    private final Integer diff;
    private final int round;

    public RoundGuessBroadcast(String username, Integer guess, Integer diff, int round) {
        this.username = username;
        this.guess    = guess;
        this.diff     = diff;
        this.round    = round;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public Integer getGuess() {
        return guess;
    }

    public Integer getDiff() {
        return diff;
    }

    public int getRound() {
        return round;
    }
}
