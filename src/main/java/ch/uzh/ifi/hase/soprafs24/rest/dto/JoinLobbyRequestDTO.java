package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class JoinLobbyRequestDTO {
    private String lobbyCode;
    private boolean friendInvited;
    private String mode;
    
    public String getLobbyCode() {
        return lobbyCode;
    }
    
    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }
    
    public boolean isFriendInvited() {
        return friendInvited;
    }
    
    public void setFriendInvited(boolean friendInvited) {
        this.friendInvited = friendInvited;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
}
