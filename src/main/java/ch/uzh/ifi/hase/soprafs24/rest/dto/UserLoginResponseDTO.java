package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserLoginResponseDTO {
    private Long userid;
    private String username;
    private String token;
    private int points; // from userProfile.mmr

    // Getters and setters
    public Long getUserid() {
        return userid;
    }
    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }
}
