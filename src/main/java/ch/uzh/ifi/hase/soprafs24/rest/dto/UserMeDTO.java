package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserMeDTO {
    private Long userid;
    private String username;
    private String email;
    private String token;
    private Boolean statsPublic;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getStatsPublic() {
        return statsPublic;
    }

    public void setStatsPublic(Boolean statsPublic) {
        this.statsPublic = statsPublic;
    }
}
