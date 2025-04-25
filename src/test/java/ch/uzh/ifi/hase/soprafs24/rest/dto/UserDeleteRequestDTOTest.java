package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserDeleteRequestDTOTest {

    private UserDeleteRequestDTO userDeleteRequestDTO;

    @BeforeEach
    void setUp() {
        userDeleteRequestDTO = new UserDeleteRequestDTO();
        userDeleteRequestDTO.setPassword("password123");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("password123", userDeleteRequestDTO.getPassword());
        
        // Test changing values
        userDeleteRequestDTO.setPassword("newPassword");
        
        assertEquals("newPassword", userDeleteRequestDTO.getPassword());
    }
    
    @Test
    void testNullValues() {
        UserDeleteRequestDTO dto = new UserDeleteRequestDTO();
        assertNull(dto.getPassword());
    }
}
