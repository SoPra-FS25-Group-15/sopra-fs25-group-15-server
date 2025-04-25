package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class JoinLobbyRequestDTOTest {

    private JoinLobbyRequestDTO joinLobbyRequestDTO;

    @BeforeEach
    void setUp() {
        joinLobbyRequestDTO = new JoinLobbyRequestDTO();
        joinLobbyRequestDTO.setLobbyCode("ABC123");
        joinLobbyRequestDTO.setFriendInvited(true);
        joinLobbyRequestDTO.setMode("team");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("ABC123", joinLobbyRequestDTO.getLobbyCode());
        assertTrue(joinLobbyRequestDTO.isFriendInvited());
        assertEquals("team", joinLobbyRequestDTO.getMode());
        
        // Test changing values
        joinLobbyRequestDTO.setLobbyCode("XYZ789");
        joinLobbyRequestDTO.setFriendInvited(false);
        joinLobbyRequestDTO.setMode("solo");
        
        assertEquals("XYZ789", joinLobbyRequestDTO.getLobbyCode());
        assertFalse(joinLobbyRequestDTO.isFriendInvited());
        assertEquals("solo", joinLobbyRequestDTO.getMode());
    }
}
