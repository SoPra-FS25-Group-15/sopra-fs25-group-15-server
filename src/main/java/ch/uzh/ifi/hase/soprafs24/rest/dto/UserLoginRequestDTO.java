package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserLoginRequestDTO {
    private String email;
    private String password;

    // Getters and setters
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
