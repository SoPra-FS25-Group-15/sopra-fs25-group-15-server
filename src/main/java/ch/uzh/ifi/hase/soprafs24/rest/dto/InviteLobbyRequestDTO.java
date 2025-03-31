package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class InviteLobbyRequestDTO {
    // If inviting a friend from the friends list, provide friendId.
    private Long friendId;
    // If inviting a non-friend, provide the lobby code.
    private String lobbyCode;

    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
    public String getLobbyCode() { return lobbyCode; }
    public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }
}

