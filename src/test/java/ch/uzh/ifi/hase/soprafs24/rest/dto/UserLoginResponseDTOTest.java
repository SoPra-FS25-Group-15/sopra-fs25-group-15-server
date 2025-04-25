package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserLoginResponseDTOTest {

    private UserLoginResponseDTO userLoginResponseDTO;

    @BeforeEach
    void setUp() {
        userLoginResponseDTO = new UserLoginResponseDTO();
        userLoginResponseDTO.setUserid(1L);
        userLoginResponseDTO.setUsername("testUser");
        userLoginResponseDTO.setToken("jwt-token-123");
        userLoginResponseDTO.setPoints(1500);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userLoginResponseDTO.getUserid());
        assertEquals("testUser", userLoginResponseDTO.getUsername());
        assertEquals("jwt-token-123", userLoginResponseDTO.getToken());
        assertEquals(1500, userLoginResponseDTO.getPoints());
        
        // Test changing values
        userLoginResponseDTO.setUserid(2L);
        userLoginResponseDTO.setUsername("loggedInUser");
        userLoginResponseDTO.setToken("new-token-456");
        userLoginResponseDTO.setPoints(1600);
        
        assertEquals(2L, userLoginResponseDTO.getUserid());
        assertEquals("loggedInUser", userLoginResponseDTO.getUsername());
        assertEquals("new-token-456", userLoginResponseDTO.getToken());
        assertEquals(1600, userLoginResponseDTO.getPoints());
    }
    
    @Test
    void testDefaultValues() {
        UserLoginResponseDTO dto = new UserLoginResponseDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertNull(dto.getToken());
        assertEquals(0, dto.getPoints());
    }
}
