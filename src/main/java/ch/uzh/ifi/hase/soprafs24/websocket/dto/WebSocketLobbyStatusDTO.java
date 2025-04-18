package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.List;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;

/**
 * Payload for the LOBBY_STATUS WebSocket message.
 */
public class WebSocketLobbyStatusDTO {

    private Long lobbyId;
    private String code;
    private UserPublicDTO host;
    private String mode;
    private String maxPlayers;
    private Integer playersPerTeam;
    private Integer roundCardsStartAmount;
    private Boolean isPrivate;
    private String status;
    private List<UserPublicDTO> players;

    public WebSocketLobbyStatusDTO() { }

    public WebSocketLobbyStatusDTO(
            Long lobbyId,
            String code,
            UserPublicDTO host,
            String mode,
            String maxPlayers,
            Integer playersPerTeam,
            Integer roundCardsStartAmount,
            Boolean isPrivate,
            String status,
            List<UserPublicDTO> players
    ) {
        this.lobbyId = lobbyId;
        this.code = code;
        this.host = host;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.playersPerTeam = playersPerTeam;
        this.roundCardsStartAmount = roundCardsStartAmount;
        this.isPrivate = isPrivate;
        this.status = status;
        this.players = players;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UserPublicDTO getHost() {
        return host;
    }

    public void setHost(UserPublicDTO host) {
        this.host = host;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<UserPublicDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<UserPublicDTO> players) {
        this.players = players;
    }
}
