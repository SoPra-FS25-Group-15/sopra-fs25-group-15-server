package ch.uzh.ifi.hase.soprafs24.rest.dto;

/**
 * Data Transfer Object for broadcasting the start of a guessing round
 * with all necessary information for clients.
 */
public class RoundStartDTO {
    
    private int round;
    private double latitude;
    private double longitude;
    private int roundTime;
    
    public int getRound() {
        return round;
    }
    
    public void setRound(int round) {
        this.round = round;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public int getRoundTime() {
        return roundTime;
    }
    
    public void setRoundTime(int roundTime) {
        this.roundTime = roundTime;
    }
}
