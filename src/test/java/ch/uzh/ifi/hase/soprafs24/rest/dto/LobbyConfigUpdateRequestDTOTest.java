package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class LobbyConfigUpdateRequestDTOTest {

    private LobbyConfigUpdateRequestDTO lobbyConfigUpdateRequestDTO;

    @BeforeEach
    void setUp() {
        lobbyConfigUpdateRequestDTO = new LobbyConfigUpdateRequestDTO();
        lobbyConfigUpdateRequestDTO.setMode("team");
        lobbyConfigUpdateRequestDTO.setMaxPlayers(8);
        lobbyConfigUpdateRequestDTO.setMaxPlayersPerTeam(2);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("team", lobbyConfigUpdateRequestDTO.getMode());
        assertEquals(Integer.valueOf(8), lobbyConfigUpdateRequestDTO.getMaxPlayers());
        assertEquals(Integer.valueOf(2), lobbyConfigUpdateRequestDTO.getMaxPlayersPerTeam());
        
        // Test changing values
        lobbyConfigUpdateRequestDTO.setMode("solo");
        lobbyConfigUpdateRequestDTO.setMaxPlayers(4);
        lobbyConfigUpdateRequestDTO.setMaxPlayersPerTeam(1);
        
        assertEquals("solo", lobbyConfigUpdateRequestDTO.getMode());
        assertEquals(Integer.valueOf(4), lobbyConfigUpdateRequestDTO.getMaxPlayers());
        assertEquals(Integer.valueOf(1), lobbyConfigUpdateRequestDTO.getMaxPlayersPerTeam());
    }
    
    @Test
    void testNullValues() {
        LobbyConfigUpdateRequestDTO dto = new LobbyConfigUpdateRequestDTO();
        assertNull(dto.getMode());
        assertNull(dto.getMaxPlayers());
        assertNull(dto.getMaxPlayersPerTeam());
    }
}
