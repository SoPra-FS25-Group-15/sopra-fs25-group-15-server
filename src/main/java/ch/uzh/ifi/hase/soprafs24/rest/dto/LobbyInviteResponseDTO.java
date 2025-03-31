package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyInviteResponseDTO {
    // Present only for non-friend invites
    private String lobbyCode;
    // Present only for friend invites
    private String invitedFriend;

    public LobbyInviteResponseDTO() {
    }

    public LobbyInviteResponseDTO(String lobbyCode, String invitedFriend) {
        this.lobbyCode = lobbyCode;
        this.invitedFriend = invitedFriend;
    }

    public String getLobbyCode() {
        return lobbyCode;
    }

    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }

    public String getInvitedFriend() {
        return invitedFriend;
    }

    public void setInvitedFriend(String invitedFriend) {
        this.invitedFriend = invitedFriend;
    }
}


