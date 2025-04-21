package ch.uzh.ifi.hase.soprafs24.websocket.dto;

public class RoundGuessBroadcast {
    private final String type = "ROUND_GUESS";
    private final String username;
    private final Double latitude;
    private final Double longitude;
    private final Integer distance; // Distance in meters
    private final int round;

    // Constructor for geographical guesses
    public RoundGuessBroadcast(String username, Double latitude, Double longitude, Integer distance, int round) {
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
        this.round = round;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public Double getLatitude() {
        return latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }

    public Integer getDistance() {
        return distance;
    }

    public int getRound() {
        return round;
    }
}
