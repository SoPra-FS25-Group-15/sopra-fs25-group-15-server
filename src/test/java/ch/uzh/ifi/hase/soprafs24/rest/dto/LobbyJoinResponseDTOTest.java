package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LobbyJoinResponseDTOTest {

    @Test
    void testDefaultConstructor() {
        LobbyJoinResponseDTO dto = new LobbyJoinResponseDTO();
        assertNull(dto.getMessage());
        assertNull(dto.getLobby());
    }

    @Test
    void testParameterizedConstructor() {
        LobbyResponseDTO mockLobby = new LobbyResponseDTO();
        mockLobby.setLobbyId(123L);
        
        LobbyJoinResponseDTO dto = new LobbyJoinResponseDTO("User joined lobby", mockLobby);
        assertEquals("User joined lobby", dto.getMessage());
        assertNotNull(dto.getLobby());
        assertEquals(123L, dto.getLobby().getLobbyId());
    }
    
    @Test
    void testGettersAndSetters() {
        LobbyResponseDTO mockLobby = new LobbyResponseDTO();
        mockLobby.setLobbyId(123L);
        
        LobbyJoinResponseDTO dto = new LobbyJoinResponseDTO();
        dto.setMessage("User joined lobby");
        dto.setLobby(mockLobby);
        
        assertEquals("User joined lobby", dto.getMessage());
        assertNotNull(dto.getLobby());
        assertEquals(123L, dto.getLobby().getLobbyId());
        
        // Test changing values
        LobbyResponseDTO newMockLobby = new LobbyResponseDTO();
        newMockLobby.setLobbyId(456L);
        
        dto.setMessage("User joined lobby successfully");
        dto.setLobby(newMockLobby);
        
        assertEquals("User joined lobby successfully", dto.getMessage());
        assertEquals(456L, dto.getLobby().getLobbyId());
    }
}
