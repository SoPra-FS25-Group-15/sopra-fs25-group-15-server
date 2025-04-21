package ch.uzh.ifi.hase.soprafs24.websocket.dto;

/**
 * DTO for transmitting round start details to clients.
 * Includes coordinates, time information, and explicit timer control.
 */
public class RoundStartDTO {
    
    private int round;
    private double latitude;
    private double longitude;
    private int roundTime;
    private boolean startTimer = true;  // Default to true - clients should start timer unless specified otherwise
    
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
    
    public boolean isStartTimer() {
        return startTimer;
    }
    
    public void setStartTimer(boolean startTimer) {
        this.startTimer = startTimer;
    }
}
