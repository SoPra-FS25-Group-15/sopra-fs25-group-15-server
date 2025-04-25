package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginRequestDTOTest {

    private UserLoginRequestDTO userLoginRequestDTO;

    @BeforeEach
    void setUp() {
        userLoginRequestDTO = new UserLoginRequestDTO();
        userLoginRequestDTO.setEmail("test@example.com");
        userLoginRequestDTO.setPassword("password123");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("test@example.com", userLoginRequestDTO.getEmail());
        assertEquals("password123", userLoginRequestDTO.getPassword());
        
        // Test changing values
        userLoginRequestDTO.setEmail("changed@example.com");
        userLoginRequestDTO.setPassword("newPassword");
        
        assertEquals("changed@example.com", userLoginRequestDTO.getEmail());
        assertEquals("newPassword", userLoginRequestDTO.getPassword());
    }
    
    @Test
    void testNullValues() {
        UserLoginRequestDTO dto = new UserLoginRequestDTO();
        assertNull(dto.getEmail());
        assertNull(dto.getPassword());
    }
}
