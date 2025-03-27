package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserUpdateRequestDTO {
    private String username;
    private String email;
    private Boolean statsPublic;  

    // Getters and setters
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Boolean getStatsPublic() {
        return statsPublic;
    }
    public void setStatsPublic(Boolean statsPublic) {
        this.statsPublic = statsPublic;
    }
}
