package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LobbyInviteResponseDTOTest {

    @Test
    void testDefaultConstructor() {
        LobbyInviteResponseDTO dto = new LobbyInviteResponseDTO();
        assertNull(dto.getLobbyCode());
        assertNull(dto.getInvitedFriend());
    }

    @Test
    void testParameterizedConstructor() {
        LobbyInviteResponseDTO dto = new LobbyInviteResponseDTO("ABC123", "friendName");
        assertEquals("ABC123", dto.getLobbyCode());
        assertEquals("friendName", dto.getInvitedFriend());
    }
    
    @Test
    void testGettersAndSetters() {
        LobbyInviteResponseDTO dto = new LobbyInviteResponseDTO();
        dto.setLobbyCode("XYZ789");
        dto.setInvitedFriend("testFriend");
        
        assertEquals("XYZ789", dto.getLobbyCode());
        assertEquals("testFriend", dto.getInvitedFriend());
        
        // Test changing values
        dto.setLobbyCode("DEF456");
        dto.setInvitedFriend("anotherFriend");
        
        assertEquals("DEF456", dto.getLobbyCode());
        assertEquals("anotherFriend", dto.getInvitedFriend());
    }

    @Test
    void testNonFriendInvite() {
        LobbyInviteResponseDTO dto = new LobbyInviteResponseDTO("ABC123", null);
        assertEquals("ABC123", dto.getLobbyCode());
        assertNull(dto.getInvitedFriend());
    }

    @Test
    void testFriendInvite() {
        LobbyInviteResponseDTO dto = new LobbyInviteResponseDTO(null, "friendName");
        assertNull(dto.getLobbyCode());
        assertEquals("friendName", dto.getInvitedFriend());
    }
}
