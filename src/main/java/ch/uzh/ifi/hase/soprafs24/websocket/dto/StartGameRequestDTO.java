package ch.uzh.ifi.hase.soprafs24.websocket.dto;

/**
 * DTO for game start request parameters
 */
public class StartGameRequestDTO {
    private int roundTime; // seconds
    
    public int getRoundTime() {
        return roundTime;
    }
    
    public void setRoundTime(int roundTime) {
        this.roundTime = roundTime;
    }
}
