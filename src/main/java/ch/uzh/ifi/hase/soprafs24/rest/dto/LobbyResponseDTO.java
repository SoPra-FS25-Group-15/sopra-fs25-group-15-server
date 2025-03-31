package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LobbyResponseDTO {
    private Long lobbyId;
    private String lobbyName;
    private String mode;
    private String gameType;
    private String lobbyType;
    private String lobbyCode;
    private int maxPlayersPerTeam;
    private Integer maxPlayers; // Added field for solo mode
    // The round cards each player starts with.
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
    public String getLobbyType() {
        return lobbyType;
    }
    public void setLobbyType(String lobbyType) {
        this.lobbyType = lobbyType;
    }
    public String getLobbyCode() {
        return lobbyCode;
    }
    public void setLobbyCode(String lobbyCode) {
        this.lobbyCode = lobbyCode;
    }
    public int getMaxPlayersPerTeam() {
        return maxPlayersPerTeam;
    }
    public void setMaxPlayersPerTeam(int maxPlayersPerTeam) {
        this.maxPlayersPerTeam = maxPlayersPerTeam;
    }
    public Integer getMaxPlayers() {
        return maxPlayers;
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