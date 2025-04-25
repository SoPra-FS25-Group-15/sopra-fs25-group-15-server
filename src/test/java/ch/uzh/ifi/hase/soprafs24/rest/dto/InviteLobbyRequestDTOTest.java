package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class InviteLobbyRequestDTOTest {

    private InviteLobbyRequestDTO inviteLobbyRequestDTO;

    @BeforeEach
    void setUp() {
        inviteLobbyRequestDTO = new InviteLobbyRequestDTO();
    }

    @Test
    void testFriendInvite() {
        inviteLobbyRequestDTO.setFriendId(1L);
        
        assertEquals(1L, inviteLobbyRequestDTO.getFriendId());
        assertNull(inviteLobbyRequestDTO.getLobbyCode());
    }
    
    @Test
    void testNonFriendInvite() {
        inviteLobbyRequestDTO.setLobbyCode("ABC123");
        
        assertEquals("ABC123", inviteLobbyRequestDTO.getLobbyCode());
        assertNull(inviteLobbyRequestDTO.getFriendId());
    }
    
    @Test
    void testGettersAndSetters() {
        // Test setting both values
        inviteLobbyRequestDTO.setFriendId(2L);
        inviteLobbyRequestDTO.setLobbyCode("DEF456");
        
        assertEquals(2L, inviteLobbyRequestDTO.getFriendId());
        assertEquals("DEF456", inviteLobbyRequestDTO.getLobbyCode());
        
        // Test changing values
        inviteLobbyRequestDTO.setFriendId(3L);
        inviteLobbyRequestDTO.setLobbyCode("GHI789");
        
        assertEquals(3L, inviteLobbyRequestDTO.getFriendId());
        assertEquals("GHI789", inviteLobbyRequestDTO.getLobbyCode());
    }
}
