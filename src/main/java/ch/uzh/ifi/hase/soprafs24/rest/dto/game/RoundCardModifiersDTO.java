package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

public class RoundCardModifiersDTO {
    private int time;
    private int guesses; // Changed from CoordinateDTO to int
    private String streetview;
    
    public int getTime() {
        return time;
    }
    
    public void setTime(int time) {
        this.time = time;
    }
    
    public int getGuesses() {
        return guesses;
    }
    
    public void setGuesses(int guesses) {
        this.guesses = guesses;
    }
    
    public String getStreetview() {
        return streetview;
    }
    
    public void setStreetview(String streetview) {
        this.streetview = streetview;
    }
}
