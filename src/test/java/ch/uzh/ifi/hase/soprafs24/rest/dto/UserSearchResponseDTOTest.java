package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserSearchResponseDTOTest {

    private UserSearchResponseDTO userSearchResponseDTO;

    @BeforeEach
    void setUp() {
        userSearchResponseDTO = new UserSearchResponseDTO();
        userSearchResponseDTO.setUserid(1L);
        userSearchResponseDTO.setUsername("testUser");
        userSearchResponseDTO.setEmail("test@example.com");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userSearchResponseDTO.getUserid());
        assertEquals("testUser", userSearchResponseDTO.getUsername());
        assertEquals("test@example.com", userSearchResponseDTO.getEmail());
        
        // Test changing values
        userSearchResponseDTO.setUserid(2L);
        userSearchResponseDTO.setUsername("foundUser");
        userSearchResponseDTO.setEmail("found@example.com");
        
        assertEquals(2L, userSearchResponseDTO.getUserid());
        assertEquals("foundUser", userSearchResponseDTO.getUsername());
        assertEquals("found@example.com", userSearchResponseDTO.getEmail());
    }
    
    @Test
    void testNullValues() {
        UserSearchResponseDTO dto = new UserSearchResponseDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertNull(dto.getEmail());
    }
}
