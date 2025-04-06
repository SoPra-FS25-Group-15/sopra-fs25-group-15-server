package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LobbyResponseDTO {
    private Long lobbyId;
    private String lobbyName;
    private String mode;
    private String gameType;
    private boolean isPrivate;
    private String lobbyCode;
    
    // Used for team mode only
    private Integer maxPlayersPerTeam;
    
    // Used for solo mode only
    private Integer maxPlayers;
    
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
    public String getLobbyName() {
        return lobbyName;
    }
    public void setLobbyName(String lobbyName) {
        this.lobbyName = lobbyName;
    }
    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public String getGameType() {
        return gameType;
    }
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    public boolean isPrivate() {
        return isPrivate;
    }
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    public String getLobbyCode() {
        return lobbyCode;
    }
    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }
    
    // Only return maxPlayersPerTeam for team mode
    public Integer getMaxPlayersPerTeam() {
        return "team".equals(mode) ? maxPlayersPerTeam : null;
    }
    
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }
    
    // Only return maxPlayers for solo mode
    public Integer getMaxPlayers() {
        return "solo".equals(mode) ? maxPlayers : null;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public List<String> getRoundCards() {
        return roundCards;
    }
    public void setRoundCards(List<String> roundCards) {
        this.roundCards = roundCards;
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