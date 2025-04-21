package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundWinnerBroadcast {
    private final String type = "ROUND_WINNER";
    private final String winnerUsername;
    private final int round;
    private final int distance; // Added distance field

    // Original constructor for backward compatibility
    public RoundWinnerBroadcast(String winnerUsername, int round) {
        this.winnerUsername = winnerUsername;
        this.round = round;
        this.distance = 0; // Default value
    }
    
    // New constructor with distance parameter
    public RoundWinnerBroadcast(String winnerUsername, int round, int distance) {
        this.winnerUsername = winnerUsername;
        this.round = round;
        this.distance = distance;
    }

    public String getType() {
        return type;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public int getRound() {
        return round;
    }
    
    public int getDistance() {
        return distance;
    }
}
