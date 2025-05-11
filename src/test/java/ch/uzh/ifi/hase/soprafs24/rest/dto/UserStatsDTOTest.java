package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class UserStatsDTOTest {

    private UserStatsDTO userStatsDTO;

    @BeforeEach
    void setUp() {
        userStatsDTO = new UserStatsDTO();
        userStatsDTO.setGamesPlayed(10);
        userStatsDTO.setWins(5);
        userStatsDTO.setXp(1500);  // Changed from setMmr to setXp
        userStatsDTO.setPoints(1500);
    }

    @Test
    void testGettersAndSetters() {
        assertEquals(10, userStatsDTO.getGamesPlayed());
        assertEquals(5, userStatsDTO.getWins());
        assertEquals(1500, userStatsDTO.getXp());  // Changed from getMmr to getXp
        assertEquals(1500, userStatsDTO.getPoints());
        
        // Test changing values
        userStatsDTO.setGamesPlayed(15);
        userStatsDTO.setWins(8);
        userStatsDTO.setXp(1600);  // Changed from setMmr to setXp
        userStatsDTO.setPoints(1600);
        
        assertEquals(15, userStatsDTO.getGamesPlayed());
        assertEquals(8, userStatsDTO.getWins());
        assertEquals(1600, userStatsDTO.getXp());  // Changed from getMmr to getXp
        assertEquals(1600, userStatsDTO.getPoints());
    }
    
    @Test
    void testDefaultValues() {
        UserStatsDTO dto = new UserStatsDTO();
        assertEquals(0, dto.getGamesPlayed());
        assertEquals(0, dto.getWins());
        assertEquals(0, dto.getXp());  // Changed from getMmr to getXp
        assertEquals(0, dto.getPoints());
    }
}
