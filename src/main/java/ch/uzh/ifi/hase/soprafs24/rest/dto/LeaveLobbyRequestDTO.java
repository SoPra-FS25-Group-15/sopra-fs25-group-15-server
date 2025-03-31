package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaveLobbyRequestDTO {
    private Long userId; // The ID of the user who is leaving (or being kicked)

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

