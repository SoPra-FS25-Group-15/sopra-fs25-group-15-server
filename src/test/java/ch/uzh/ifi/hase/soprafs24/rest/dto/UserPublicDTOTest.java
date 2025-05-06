package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserPublicDTOTest {

    private UserPublicDTO userPublicDTO;
    private final List<String> testAchievements = Arrays.asList("First Win", "Sharp Shooter");

    @BeforeEach
    void setUp() {
        userPublicDTO = new UserPublicDTO();
        userPublicDTO.setUserid(1L);
        userPublicDTO.setUsername("testUser");
        userPublicDTO.setXp(1500);  // Changed from setMmr to setXp
        userPublicDTO.setPoints(1500);
        userPublicDTO.setAchievements(testAchievements);
        userPublicDTO.setEmail("test@example.com");
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(1L, userPublicDTO.getUserid());
        assertEquals("testUser", userPublicDTO.getUsername());
        assertEquals(1500, userPublicDTO.getXp());  // Changed from getMmr to getXp
        assertEquals(1500, userPublicDTO.getPoints());
        assertEquals(testAchievements, userPublicDTO.getAchievements());
        assertEquals("test@example.com", userPublicDTO.getEmail());
        
        // Test changing values
        List<String> newAchievements = Arrays.asList("Tournament Win", "MVP");
        userPublicDTO.setUserid(2L);
        userPublicDTO.setUsername("publicUser");
        userPublicDTO.setXp(1600);  // Changed from setMmr to setXp
        userPublicDTO.setPoints(1600);
        userPublicDTO.setAchievements(newAchievements);
        userPublicDTO.setEmail("public@example.com");
        
        assertEquals(2L, userPublicDTO.getUserid());
        assertEquals("publicUser", userPublicDTO.getUsername());
        assertEquals(1600, userPublicDTO.getXp());  // Changed from getMmr to getXp
        assertEquals(1600, userPublicDTO.getPoints());
        assertEquals(newAchievements, userPublicDTO.getAchievements());
        assertEquals("public@example.com", userPublicDTO.getEmail());
    }
    
    @Test
    void testNullValues() {
        UserPublicDTO dto = new UserPublicDTO();
        assertNull(dto.getUserid());
        assertNull(dto.getUsername());
        assertEquals(0, dto.getXp());  // Changed from getMmr to getXp
        assertEquals(0, dto.getPoints());
        assertNull(dto.getAchievements());
        assertNull(dto.getEmail());
    }
}
