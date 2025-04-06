package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserSearchResponseDTO {
    private Long userid;
    private String username;
    private String email;
    
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
}
