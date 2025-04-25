package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserPostDTOTest {

    private UserPostDTO userPostDTO;

    @BeforeEach
    void setUp() {
        userPostDTO = new UserPostDTO();
        userPostDTO.setName("John Doe");
        userPostDTO.setUsername("johndoe");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals("John Doe", userPostDTO.getName());
        assertEquals("johndoe", userPostDTO.getUsername());
        
        // Test changing values
        userPostDTO.setName("Jane Doe");
        userPostDTO.setUsername("janedoe");
        
        assertEquals("Jane Doe", userPostDTO.getName());
        assertEquals("janedoe", userPostDTO.getUsername());
    }
    
    @Test
    void testNullValues() {
        UserPostDTO dto = new UserPostDTO();
        assertNull(dto.getName());
        assertNull(dto.getUsername());
    }
}
