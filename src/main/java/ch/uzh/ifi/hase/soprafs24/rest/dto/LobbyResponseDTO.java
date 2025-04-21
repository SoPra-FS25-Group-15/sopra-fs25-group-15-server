package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LobbyResponseDTO {
    private Long lobbyId;
    private String mode;
    private boolean isPrivate;
    // New field name as per requirements
    private String code;
    // For backward compatibility
    private String lobbyCode;
    
    // Used for team mode only, renamed from maxPlayersPerTeam
    private Integer playersPerTeam;
    
    // Changed to String as per requirements
    private String maxPlayers;
    
    // New field for count of round cards instead of the list
    private Integer roundCardsStartAmount;
    
    // Keep for backward compatibility
    private List<String> roundCards;
    
    private Instant createdAt;
    private String status;
    
    // Team mode mapping
    private Map<String, List<UserPublicDTO>> teams;
    
    // Solo mode players list
    private List<UserPublicDTO> players;

    public Long getLobbyId() {
        return lobbyId;
    }
    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public boolean isPrivate() {
        return isPrivate;
    }
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
    // New getter/setter for code
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    
    // For backward compatibility
    public String getLobbyCode() {
        return code;
    }
    public void setLobbyCode(String lobbyCode) {
        this.code = lobbyCode;
        this.lobbyCode = lobbyCode;
    }
    
    // Fix: Remove conditional check that was causing tests to fail
    public Integer getPlayersPerTeam() {
        return playersPerTeam;
    }
    
    // Changed parameter type from primitive int to object Integer for consistency
    public void setPlayersPerTeam(Integer playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }
    
    // Changed to String
    public String getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(String maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    // For backward compatibility
    public void setMaxPlayers(Integer maxPlayers) {
        if (maxPlayers != null) {
            this.maxPlayers = String.valueOf(maxPlayers);
        }
    }
    
    // New getter/setter for roundCardsStartAmount
    public Integer getRoundCardsStartAmount() {
        return roundCardsStartAmount;
    }
    public void setRoundCardsStartAmount(Integer roundCardsStartAmount) {
        this.roundCardsStartAmount = roundCardsStartAmount;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Map<String, List<UserPublicDTO>> getTeams() {
        return teams;
    }
    public void setTeams(Map<String, List<UserPublicDTO>> teams) {
        this.teams = teams;
    }
    public List<UserPublicDTO> getPlayers() {
        return players;
    }
    public void setPlayers(List<UserPublicDTO> players) {
        this.players = players;
    }
}