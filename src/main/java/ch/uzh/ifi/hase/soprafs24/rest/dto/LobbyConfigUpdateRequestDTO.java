package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class LobbyConfigUpdateRequestDTO {
    // Allowed values: "solo" or "team"
    private String mode;
    // Maximum players per team; must be 1 for solo or 2 for team
    private int maxPlayersPerTeam;
    // The round cards each player starts with (stored in lobby_hints)
    private List<String> roundCards;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public int getMaxPlayersPerTeam() { return maxPlayersPerTeam; }
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) { this.maxPlayersPerTeam = maxPlayersPerTeam; }
    public List<String> getRoundCards() { return roundCards; }
    public void setRoundCards(List<String> roundCards) { this.roundCards = roundCards; }
}

