package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserMeDTOTest {

    private UserMeDTO userMeDTO;

    @BeforeEach
    void setUp() {
        userMeDTO = new UserMeDTO();
        userMeDTO.setUserid(1L);
        userMeDTO.setUsername("testUser");
        userMeDTO.setEmail("test@example.com");
        userMeDTO.setToken("jwt-token-123");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userMeDTO.getUserid());
        assertEquals("testUser", userMeDTO.getUsername());
        assertEquals("test@example.com", userMeDTO.getEmail());
        assertEquals("jwt-token-123", userMeDTO.getToken());
        
        // Test changing values
        userMeDTO.setUserid(2L);
        userMeDTO.setUsername("currentUser");
        userMeDTO.setEmail("current@example.com");
        userMeDTO.setToken("new-token-456");
        
        assertEquals(2L, userMeDTO.getUserid());
        assertEquals("currentUser", userMeDTO.getUsername());
        assertEquals("current@example.com", userMeDTO.getEmail());
        assertEquals("new-token-456", userMeDTO.getToken());
    }
    
    @Test
    void testNullValues() {
        UserMeDTO dto = new UserMeDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
        assertNull(dto.getToken());
    }
}
