package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

public class LobbyRequestDTO {
    private String lobbyName;
    // "ranked" or "unranked"
    private String gameType;
    // Indicates if the lobby is private (true) or public (false)
    private boolean isPrivate;
    private int maxPlayersPerTeam;
    // Optionally, initial round cards (stored in lobby_hints)
    private List<String> hintsEnabled;
    // Optionally, teams (typically empty at creation)
    private Map<String, List<Long>> teams;
    
    // New: game mode provided by client ("solo" or "team")
    private String mode;

    public String getLobbyName() { return lobbyName; }
    public void setLobbyName(String lobbyName) { this.lobbyName = lobbyName; }
    public String getGameType() { return gameType; }
    public void setGameType(String gameType) { this.gameType = gameType; }
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) { this.maxPlayersPerTeam = maxPlayersPerTeam; }
    public List<String> getHintsEnabled() { return hintsEnabled; }
    public void setHintsEnabled(List<String> hintsEnabled) { this.hintsEnabled = hintsEnabled; }
    public Map<String, List<Long>> getTeams() { return teams; }
    public void setTeams(Map<String, List<Long>> teams) { this.teams = teams; }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}

