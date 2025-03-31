package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class JoinLobbyRequestDTO {
    private String lobbyCode;
    private boolean friendInvited;
    // Determines the lobby join mode: "solo" or "team" (use LobbyConstants.MODE_SOLO / MODE_TEAM)
    private String mode;
    // Optional: if mode is "team", provide the team name.
    private String team;

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

    public String getTeam() {
        return team;
    }
    public void setTeam(String team) {
        this.team = team;
    }
}
