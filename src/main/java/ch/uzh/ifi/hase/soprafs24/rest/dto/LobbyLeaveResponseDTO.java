package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyLeaveResponseDTO {
    private String message;
    private LobbyResponseDTO lobby;

    public LobbyLeaveResponseDTO() {}

    public LobbyLeaveResponseDTO(String message, LobbyResponseDTO lobby) {
        this.message = message;
        this.lobby = lobby;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LobbyResponseDTO getLobby() { return lobby; }
    public void setLobby(LobbyResponseDTO lobby) { this.lobby = lobby; }
}

