package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LobbyConfigUpdateRequestDTO {
    // Allowed values: "solo" or "team"
    private String mode;
    // Maximum total players allowed in the lobby
    private Integer maxPlayers;
    // Maximum players per team
    private Integer maxPlayersPerTeam;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Integer getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) { this.maxPlayersPerTeam = maxPlayersPerTeam; }
}

