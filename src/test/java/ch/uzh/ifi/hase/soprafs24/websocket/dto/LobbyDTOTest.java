package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LobbyDTOTest {

    @Test
    void testInitialState() {
        LobbyDTO dto = new LobbyDTO();
        assertNull(dto.getCode(), "Code should be null initially.");
        assertNull(dto.getMaxPlayers(), "MaxPlayers should be null initially.");
        assertNull(dto.getPlayersPerTeam(), "PlayersPerTeam should be null initially.");
        assertNull(dto.getRoundCardsStartAmount(), "RoundCardsStartAmount should be null initially.");
    }

    @Test
    void testSetAndGetCode() {
        LobbyDTO dto = new LobbyDTO();
        String code = "LOBBY123";
        dto.setCode(code);
        assertEquals(code, dto.getCode(), "getCode should return the set code.");
    }

    @Test
    void testSetAndGetMaxPlayers() {
        LobbyDTO dto = new LobbyDTO();
        String maxPlayers = "10";
        dto.setMaxPlayers(maxPlayers);
        assertEquals(maxPlayers, dto.getMaxPlayers(), "getMaxPlayers should return the set maxPlayers.");
    }

    @Test
    void testSetAndGetPlayersPerTeam() {
        LobbyDTO dto = new LobbyDTO();
        Integer playersPerTeam = 2;
        dto.setPlayersPerTeam(playersPerTeam);
        assertEquals(playersPerTeam, dto.getPlayersPerTeam(), "getPlayersPerTeam should return the set playersPerTeam.");
    }

    @Test
    void testSetAndGetRoundCardsStartAmount() {
        LobbyDTO dto = new LobbyDTO();
        Integer roundCardsStartAmount = 5;
        dto.setRoundCardsStartAmount(roundCardsStartAmount);
        assertEquals(roundCardsStartAmount, dto.getRoundCardsStartAmount(), "getRoundCardsStartAmount should return the set roundCardsStartAmount.");
    }

    @Test
    void testSettersWithNullValues() {
        LobbyDTO dto = new LobbyDTO();

        dto.setCode(null);
        assertNull(dto.getCode(), "getCode should return null if null is set for code.");

        dto.setMaxPlayers(null);
        assertNull(dto.getMaxPlayers(), "getMaxPlayers should return null if null is set for maxPlayers.");

        dto.setPlayersPerTeam(null);
        assertNull(dto.getPlayersPerTeam(), "getPlayersPerTeam should return null if null is set for playersPerTeam.");

        dto.setRoundCardsStartAmount(null);
        assertNull(dto.getRoundCardsStartAmount(), "getRoundCardsStartAmount should return null if null is set for roundCardsStartAmount.");
    }
}