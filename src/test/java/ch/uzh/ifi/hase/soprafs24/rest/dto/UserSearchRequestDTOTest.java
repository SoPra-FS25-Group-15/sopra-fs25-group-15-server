package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserSearchRequestDTOTest {

    private UserSearchRequestDTO userSearchRequestDTO;

    @BeforeEach
    void setUp() {
        userSearchRequestDTO = new UserSearchRequestDTO();
        userSearchRequestDTO.setEmail("search@example.com");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("search@example.com", userSearchRequestDTO.getEmail());
        
        // Test changing values
        userSearchRequestDTO.setEmail("another@example.com");
        
        assertEquals("another@example.com", userSearchRequestDTO.getEmail());
    }
    
    @Test
    void testNullValues() {
        UserSearchRequestDTO dto = new UserSearchRequestDTO();
        assertNull(dto.getEmail());
    }
}
