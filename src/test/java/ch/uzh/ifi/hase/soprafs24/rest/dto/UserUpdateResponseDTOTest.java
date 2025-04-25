package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserUpdateResponseDTOTest {

    private UserUpdateResponseDTO userUpdateResponseDTO;

    @BeforeEach
    void setUp() {
        userUpdateResponseDTO = new UserUpdateResponseDTO();
        userUpdateResponseDTO.setUserid(1L);
        userUpdateResponseDTO.setUsername("testUser");
        userUpdateResponseDTO.setEmail("test@example.com");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userUpdateResponseDTO.getUserid());
        assertEquals("testUser", userUpdateResponseDTO.getUsername());
        assertEquals("test@example.com", userUpdateResponseDTO.getEmail());
        
        // Test changing values
        userUpdateResponseDTO.setUserid(2L);
        userUpdateResponseDTO.setUsername("updatedUser");
        userUpdateResponseDTO.setEmail("updated@example.com");
        
        assertEquals(2L, userUpdateResponseDTO.getUserid());
        assertEquals("updatedUser", userUpdateResponseDTO.getUsername());
        assertEquals("updated@example.com", userUpdateResponseDTO.getEmail());
    }
    
    @Test
    void testNullValues() {
        UserUpdateResponseDTO dto = new UserUpdateResponseDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
    }
}
