package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;
import java.util.Map;

public class LobbyRequestDTO {
    // Indicates if the lobby is private (true) or public (false)
    private boolean isPrivate;
    // For team mode: players per team (default 2)
    // For solo mode: always 1 (each player is their own team)
    private Integer maxPlayersPerTeam;
    // Maximum total players in the lobby (default 8)
    private Integer maxPlayers;
    // Optionally, initial round cards (stored in lobby_hints)
    private List<String> hintsEnabled;
    // Optionally, teams (typically empty at creation)
    private Map<String, List<Long>> teams;
    
    // Game mode provided by client ("solo" or "team")
    private String mode;

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
    public Integer getMaxPlayersPerTeam() { 
        // For solo mode, always return 1
        if ("solo".equalsIgnoreCase(mode)) {
            return 1;
        }
        // For team mode, return the configured value or default
        return maxPlayersPerTeam != null ? maxPlayersPerTeam : 2; 
    }
    public void setMaxPlayersPerTeam(Integer maxPlayersPerTeam) { 
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }
    public Integer getMaxPlayers() {
        return maxPlayers != null ? maxPlayers : 8; // Default to 8 if not specified
    }
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    public List<String> getHintsEnabled() { return hintsEnabled; }
    public void setHintsEnabled(List<String> hintsEnabled) { this.hintsEnabled = hintsEnabled; }
    public Map<String, List<Long>> getTeams() { return teams; }
    public void setTeams(Map<String, List<Long>> teams) { this.teams = teams; }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
}

