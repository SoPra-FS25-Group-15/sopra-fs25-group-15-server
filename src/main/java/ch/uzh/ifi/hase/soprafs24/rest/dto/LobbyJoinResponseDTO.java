package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyJoinResponseDTO {
    private String message;
    private LobbyResponseDTO lobby;
    
    // Default constructor for JSON deserialization
    public LobbyJoinResponseDTO() {
    }
    
    // Constructor with parameters
    public LobbyJoinResponseDTO(String message, LobbyResponseDTO lobby) {
        this.message = message;
        this.lobby = lobby;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LobbyResponseDTO getLobby() {
        return lobby;
    }
    
    public void setLobby(LobbyResponseDTO lobby) {
        this.lobby = lobby;
    }
}

