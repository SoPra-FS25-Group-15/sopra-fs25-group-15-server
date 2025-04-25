package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserRegisterDTOTest {

    private UserRegisterRequestDTO userRegisterDTO;

    @BeforeEach
    void setUp() {
        userRegisterDTO = new UserRegisterRequestDTO();
        userRegisterDTO.setUsername("newUser");
        userRegisterDTO.setPassword("securePassword");
        userRegisterDTO.setEmail("user@example.com");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("newUser", userRegisterDTO.getUsername());
        assertEquals("securePassword", userRegisterDTO.getPassword());
        assertEquals("user@example.com", userRegisterDTO.getEmail());
        
        // Test changing values
        userRegisterDTO.setUsername("updatedUser");
        userRegisterDTO.setPassword("newPassword");
        userRegisterDTO.setEmail("updated@example.com");
        
        assertEquals("updatedUser", userRegisterDTO.getUsername());
        assertEquals("newPassword", userRegisterDTO.getPassword());
        assertEquals("updated@example.com", userRegisterDTO.getEmail());
    }
    
    @Test
    void testNullValues() {
        UserRegisterRequestDTO dto = new UserRegisterRequestDTO();
        assertNull(dto.getUsername());
        assertNull(dto.getPassword());
        assertNull(dto.getEmail());
    }
}
