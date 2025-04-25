package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UserRegisterResponseDTOTest {

    private UserRegisterResponseDTO userRegisterResponseDTO;
    private final Instant testInstant = Instant.parse("2023-01-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        userRegisterResponseDTO = new UserRegisterResponseDTO();
        userRegisterResponseDTO.setUserid(1L);
        userRegisterResponseDTO.setUsername("newUser");
        userRegisterResponseDTO.setEmail("user@example.com");
        userRegisterResponseDTO.setToken("jwt-token-123");
        userRegisterResponseDTO.setCreatedAt(testInstant);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userRegisterResponseDTO.getUserid());
        assertEquals("newUser", userRegisterResponseDTO.getUsername());
        assertEquals("user@example.com", userRegisterResponseDTO.getEmail());
        assertEquals("jwt-token-123", userRegisterResponseDTO.getToken());
        assertEquals(testInstant, userRegisterResponseDTO.getCreatedAt());
        
        // Test changing values
        Instant newInstant = Instant.parse("2023-02-01T12:00:00Z");
        userRegisterResponseDTO.setUserid(2L);
        userRegisterResponseDTO.setUsername("updatedUser");
        userRegisterResponseDTO.setEmail("updated@example.com");
        userRegisterResponseDTO.setToken("new-token-456");
        userRegisterResponseDTO.setCreatedAt(newInstant);
        
        assertEquals(2L, userRegisterResponseDTO.getUserid());
        assertEquals("updatedUser", userRegisterResponseDTO.getUsername());
        assertEquals("updated@example.com", userRegisterResponseDTO.getEmail());
        assertEquals("new-token-456", userRegisterResponseDTO.getToken());
        assertEquals(newInstant, userRegisterResponseDTO.getCreatedAt());
    }
    
    @Test
    void testNullValues() {
        UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
        assertNull(dto.getToken());
        assertNull(dto.getCreatedAt());
    }
}
