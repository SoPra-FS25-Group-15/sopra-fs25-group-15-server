package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LobbyLeaveResponseDTOTest {

    @Test
    void testDefaultConstructor() {
        LobbyLeaveResponseDTO dto = new LobbyLeaveResponseDTO();
        assertNull(dto.getMessage());
        assertNull(dto.getLobby());
    }

    @Test
    void testParameterizedConstructor() {
        LobbyResponseDTO mockLobby = new LobbyResponseDTO();
        mockLobby.setLobbyId(123L);
        
        LobbyLeaveResponseDTO dto = new LobbyLeaveResponseDTO("User left lobby", mockLobby);
        assertEquals("User left lobby", dto.getMessage());
        assertNotNull(dto.getLobby());
        assertEquals(123L, dto.getLobby().getLobbyId());
    }
    
    @Test
    void testGettersAndSetters() {
        LobbyResponseDTO mockLobby = new LobbyResponseDTO();
        mockLobby.setLobbyId(123L);
        
        LobbyLeaveResponseDTO dto = new LobbyLeaveResponseDTO();
        dto.setMessage("User left lobby");
        dto.setLobby(mockLobby);
        
        assertEquals("User left lobby", dto.getMessage());
        assertNotNull(dto.getLobby());
        assertEquals(123L, dto.getLobby().getLobbyId());
        
        // Test changing values
        LobbyResponseDTO newMockLobby = new LobbyResponseDTO();
        newMockLobby.setLobbyId(456L);
        
        dto.setMessage("User left lobby successfully");
        dto.setLobby(newMockLobby);
        
        assertEquals("User left lobby successfully", dto.getMessage());
        assertEquals(456L, dto.getLobby().getLobbyId());
    }
}
