package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundStartMessage {
    private final String type = "ROUND_START";
    private final int round;
    private final double latitude;
    private final double longitude;
    private final int roundTime;

    // Constructor for geographical rounds
    public RoundStartMessage(int round, double latitude, double longitude) {
        this.round = round;
        this.latitude = latitude;
        this.longitude = longitude;
        this.roundTime = 30; // Default round time in seconds
    }

    // Constructor with custom round time
    public RoundStartMessage(int round, double latitude, double longitude, int roundTime) {
        this.round = round;
        this.latitude = latitude;
        this.longitude = longitude;
        this.roundTime = roundTime;
    }

    // Getters
    public String getType() {
        return type;
    }

    public int getRound() {
        return round;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRoundTime() {
        return roundTime;
    }
}
