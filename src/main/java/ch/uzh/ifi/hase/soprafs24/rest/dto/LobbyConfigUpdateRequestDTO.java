package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class LobbyConfigUpdateRequestDTO {
    // Allowed values: "solo" or "team"
    private String mode;
    // Maximum total players allowed in the lobby
    private Integer maxPlayers;
    // The round cards each player starts with (stored in lobby_hints)
    private List<String> roundCards;

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public List<String> getRoundCards() { return roundCards; }
    public void setRoundCards(List<String> roundCards) { this.roundCards = roundCards; }
}

