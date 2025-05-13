package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketLobbyStatusDTOTest {

    @Test
    void testDefaultConstructor() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        assertNull(dto.getLobbyId(), "Default constructor: lobbyId should be null.");
        assertNull(dto.getCode(), "Default constructor: code should be null.");
        assertNull(dto.getHost(), "Default constructor: host should be null.");
        assertNull(dto.getMode(), "Default constructor: mode should be null.");
        assertNull(dto.getMaxPlayers(), "Default constructor: maxPlayers should be null.");
        assertNull(dto.getPlayersPerTeam(), "Default constructor: playersPerTeam should be null.");
        assertNull(dto.getRoundCardsStartAmount(), "Default constructor: roundCardsStartAmount should be null.");
        assertNull(dto.getIsPrivate(), "Default constructor: isPrivate should be null.");
        assertNull(dto.getStatus(), "Default constructor: status should be null.");
        assertNull(dto.getPlayers(), "Default constructor: players list should be null.");
    }

    @Test
    void testParameterizedConstructorAndGetters() {
        Long lobbyId = 1L;
        String code = "ABCD";
        UserPublicDTO host = new UserPublicDTO(); // Assume UserPublicDTO is properly tested elsewhere
        // and has a default constructor or provide mock
        host.setUserid(10L);
        host.setUsername("HostUser");

        String mode = "STANDARD";
        String maxPlayers = "4";
        Integer playersPerTeam = 2;
        Integer roundCardsStartAmount = 7;
        Boolean isPrivate = true;
        String status = "WAITING";
        List<UserPublicDTO> players = new ArrayList<>();
        UserPublicDTO player1 = new UserPublicDTO();
        player1.setUserid(11L);
        player1.setUsername("PlayerOne");
        players.add(player1);

        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO(
                lobbyId, code, host, mode, maxPlayers, playersPerTeam,
                roundCardsStartAmount, isPrivate, status, players
        );

        assertEquals(lobbyId, dto.getLobbyId());
        assertEquals(code, dto.getCode());
        assertEquals(host, dto.getHost());
        assertEquals(mode, dto.getMode());
        assertEquals(maxPlayers, dto.getMaxPlayers());
        assertEquals(playersPerTeam, dto.getPlayersPerTeam());
        assertEquals(roundCardsStartAmount, dto.getRoundCardsStartAmount());
        assertEquals(isPrivate, dto.getIsPrivate());
        assertEquals(status, dto.getStatus());
        assertEquals(players, dto.getPlayers());
        assertNotNull(dto.getPlayers(), "Players list should not be null after parameterized construction.");
        assertEquals(1, dto.getPlayers().size(), "Players list should have one player.");
    }

    @Test
    void testSetAndGetLobbyId() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        Long lobbyId = 2L;
        dto.setLobbyId(lobbyId);
        assertEquals(lobbyId, dto.getLobbyId());
    }

    @Test
    void testSetAndGetCode() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        String code = "EFGH";
        dto.setCode(code);
        assertEquals(code, dto.getCode());
    }

    @Test
    void testSetAndGetHost() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        UserPublicDTO host = new UserPublicDTO();
        host.setUserid(20L);
        host.setUsername("NewHost");
        dto.setHost(host);
        assertEquals(host, dto.getHost());
    }

    @Test
    void testSetAndGetMode() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        String mode = "TEAM_MODE";
        dto.setMode(mode);
        assertEquals(mode, dto.getMode());
    }

    @Test
    void testSetAndGetMaxPlayers() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        String maxPlayers = "8";
        dto.setMaxPlayers(maxPlayers);
        assertEquals(maxPlayers, dto.getMaxPlayers());
    }

    @Test
    void testSetAndGetPlayersPerTeam() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        Integer playersPerTeam = 4;
        dto.setPlayersPerTeam(playersPerTeam);
        assertEquals(playersPerTeam, dto.getPlayersPerTeam());
    }

    @Test
    void testSetAndGetRoundCardsStartAmount() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        Integer amount = 5;
        dto.setRoundCardsStartAmount(amount);
        assertEquals(amount, dto.getRoundCardsStartAmount());
    }

    @Test
    void testSetAndGetIsPrivate() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        Boolean isPrivate = false;
        dto.setIsPrivate(isPrivate);
        assertEquals(isPrivate, dto.getIsPrivate());
    }

    @Test
    void testSetAndGetStatus() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        String status = "IN_GAME";
        dto.setStatus(status);
        assertEquals(status, dto.getStatus());
    }

    @Test
    void testSetAndGetPlayers() {
        WebSocketLobbyStatusDTO dto = new WebSocketLobbyStatusDTO();
        List<UserPublicDTO> players = new ArrayList<>();
        UserPublicDTO player1 = new UserPublicDTO();
        player1.setUserid(1L);
        player1.setUsername("PlayerX");
        UserPublicDTO player2 = new UserPublicDTO();
        player2.setUserid(2L);
        player2.setUsername("PlayerY");
        players.add(player1);
        players.add(player2);
        dto.setPlayers(players);

        assertEquals(players, dto.getPlayers());
        assertNotNull(dto.getPlayers());
        assertEquals(2, dto.getPlayers().size());
    }
}