package ch.uzh.ifi.hase.soprafs24.websocket.dto;

/**
 * Data transfer object for Lobby in WebSocket messages
 */
public class LobbyDTO {
    private String code;
    private String maxPlayers;
    private Integer playersPerTeam;
    private Integer roundCardsStartAmount;
    
    // Getters and setters
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(String maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public Integer getPlayersPerTeam() {
        return playersPerTeam;
    }
    
    public void setPlayersPerTeam(Integer playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }
    
    public Integer getRoundCardsStartAmount() {
        return roundCardsStartAmount;
    }
    
    public void setRoundCardsStartAmount(Integer roundCardsStartAmount) {
        this.roundCardsStartAmount = roundCardsStartAmount;
    }
}
