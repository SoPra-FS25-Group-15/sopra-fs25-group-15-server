package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LobbyRequestDTOTest {

    private LobbyRequestDTO lobbyRequestDTO;

    @BeforeEach
    void setUp() {
        lobbyRequestDTO = new LobbyRequestDTO();
        lobbyRequestDTO.setPrivate(true);
        lobbyRequestDTO.setMaxPlayersPerTeam(2);
        lobbyRequestDTO.setMaxPlayers(8);
        lobbyRequestDTO.setMode("team");
        
        Map<String, List<Long>> teams = new HashMap<>();
        List<Long> team1 = new ArrayList<>();
        team1.add(1L);
        teams.put("team1", team1);
        lobbyRequestDTO.setTeams(teams);
    }

    @Test
    void testGettersAndSetters() {
        assertTrue(lobbyRequestDTO.isPrivate());
        assertEquals(2, lobbyRequestDTO.getMaxPlayersPerTeam());
        assertEquals(8, lobbyRequestDTO.getMaxPlayers());
        assertEquals("team", lobbyRequestDTO.getMode());
        assertNotNull(lobbyRequestDTO.getTeams());
        assertTrue(lobbyRequestDTO.getTeams().containsKey("team1"));
        assertEquals(1, lobbyRequestDTO.getTeams().get("team1").size());
        assertEquals(1L, lobbyRequestDTO.getTeams().get("team1").get(0));
    }
    
    @Test
    void testDefaultValues() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        assertEquals(8, dto.getMaxPlayers()); // Default is 8
        
        dto.setMode("team");
        assertEquals(2, dto.getMaxPlayersPerTeam()); // Default for team mode is 2
        
        dto.setMode("solo");
        assertEquals(1, dto.getMaxPlayersPerTeam()); // Solo mode always returns 1
    }
    
    @Test
    void testSoloModePlayersPerTeam() {
        lobbyRequestDTO.setMode("solo");
        lobbyRequestDTO.setMaxPlayersPerTeam(3); // Should be ignored for solo mode
        assertEquals(1, lobbyRequestDTO.getMaxPlayersPerTeam()); // Always 1 for solo mode
    }
}
