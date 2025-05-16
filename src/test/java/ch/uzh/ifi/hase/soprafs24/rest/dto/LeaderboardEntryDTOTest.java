package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardEntryDTOTest {

    @Test
    void testInitialState() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        assertEquals(0, dto.getRank());
        assertNull(dto.getUserId());
        assertNull(dto.getUsername());
        assertEquals(0, dto.getXp());
        assertEquals(0, dto.getGamesPlayed());
        assertEquals(0, dto.getWins());
        assertFalse(dto.isCurrentUser());
    }

    @Test
    void testSetAndGetRank() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setRank(5);
        assertEquals(5, dto.getRank());
    }

    @Test
    void testSetAndGetUserId() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        Long userId = 123L;
        dto.setUserId(userId);
        assertEquals(userId, dto.getUserId());
    }

    @Test
    void testSetAndGetUsername() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        String username = "testUser";
        dto.setUsername(username);
        assertEquals(username, dto.getUsername());
    }

    @Test
    void testSetAndGetXp() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setXp(1000);
        assertEquals(1000, dto.getXp());
    }

    @Test
    void testSetAndGetGamesPlayed() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setGamesPlayed(42);
        assertEquals(42, dto.getGamesPlayed());
    }

    @Test
    void testSetAndGetWins() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setWins(15);
        assertEquals(15, dto.getWins());
    }

    @Test
    void testSetAndIsCurrentUser() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setCurrentUser(true);
        assertTrue(dto.isCurrentUser());
    }

    @Test
    void testFullyPopulatedDTO() {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.setRank(3);
        dto.setUserId(456L);
        dto.setUsername("champion");
        dto.setXp(2500);
        dto.setGamesPlayed(30);
        dto.setWins(20);
        dto.setCurrentUser(true);

        assertEquals(3, dto.getRank());
        assertEquals(456L, dto.getUserId());
        assertEquals("champion", dto.getUsername());
        assertEquals(2500, dto.getXp());
        assertEquals(30, dto.getGamesPlayed());
        assertEquals(20, dto.getWins());
        assertTrue(dto.isCurrentUser());
    }
}
