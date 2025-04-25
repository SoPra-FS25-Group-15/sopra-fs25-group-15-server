package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserUpdateRequestDTOTest {

    private UserUpdateRequestDTO userUpdateRequestDTO;

    @BeforeEach
    void setUp() {
        userUpdateRequestDTO = new UserUpdateRequestDTO();
        userUpdateRequestDTO.setUsername("testUser");
        userUpdateRequestDTO.setEmail("test@example.com");
        userUpdateRequestDTO.setStatsPublic(true);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("testUser", userUpdateRequestDTO.getUsername());
        assertEquals("test@example.com", userUpdateRequestDTO.getEmail());
        assertTrue(userUpdateRequestDTO.getStatsPublic());
        
        // Test changing values
        userUpdateRequestDTO.setUsername("updatedUser");
        userUpdateRequestDTO.setEmail("updated@example.com");
        userUpdateRequestDTO.setStatsPublic(false);
        
        assertEquals("updatedUser", userUpdateRequestDTO.getUsername());
        assertEquals("updated@example.com", userUpdateRequestDTO.getEmail());
        assertFalse(userUpdateRequestDTO.getStatsPublic());
    }
    
    @Test
    void testNullValues() {
        UserUpdateRequestDTO dto = new UserUpdateRequestDTO();
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
        assertNull(dto.getStatsPublic());
    }
}
