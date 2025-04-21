package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

/**
 * Data transfer object for geographic coordinates
 */
public class CoordinateDTO {
    private double lat;
    private double lon;
    
    // Default constructor
    public CoordinateDTO() {}
    
    // Constructor with parameters
    public CoordinateDTO(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
    
    public double getLat() {
        return lat;
    }
    
    public void setLat(double lat) {
        this.lat = lat;
    }
    
    public double getLon() {
        return lon;
    }
    
    public void setLon(double lon) {
        this.lon = lon;
    }
}
