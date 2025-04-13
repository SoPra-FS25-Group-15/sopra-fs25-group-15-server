package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.List;

/**
 * Data transfer object for lobby management state
 * Used to send information about a user's current lobby and pending invites
 */
public class LobbyManagementDTO {
    private String currentLobbyCode;
    private List<PendingInvite> pendingInvites;

    /**
     * Inner class representing a pending lobby invitation
     */
    public static class PendingInvite {
        private String username;
        private String lobbyCode;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getLobbyCode() {
            return lobbyCode;
        }

        public void setLobbyCode(String lobbyCode) {
            this.lobbyCode = lobbyCode;
        }
    }

    // Getters and setters
    public String getCurrentLobbyCode() {
        return currentLobbyCode;
    }

    public void setCurrentLobbyCode(String currentLobbyCode) {
        this.currentLobbyCode = currentLobbyCode;
    }

    public List<PendingInvite> getPendingInvites() {
        return pendingInvites;
    }

    public void setPendingInvites(List<PendingInvite> pendingInvites) {
        this.pendingInvites = pendingInvites;
    }
}
