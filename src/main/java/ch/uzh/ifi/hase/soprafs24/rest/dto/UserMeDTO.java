package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserMeDTO {
    private Long userid;
    private String username;
    private String email;
    private String token;
    // Possibly more fields if you want them, like mmr or achievements

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
}
