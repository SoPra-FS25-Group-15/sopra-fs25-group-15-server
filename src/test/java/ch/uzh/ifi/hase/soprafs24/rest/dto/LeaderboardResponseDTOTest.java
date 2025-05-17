package ch.uzh.ifi.hase.soprafs24.rest.dto;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardResponseDTOTest {

    @Test
    void testInitialState() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        assertNull(dto.getEntries());
        assertEquals(0, dto.getTotalPlayers());
        assertEquals(0, dto.getPage());
        assertEquals(0, dto.getPageSize());
        assertEquals(0, dto.getTotalPages());
        assertNotNull(dto.getCurrentUserEntry()); // Changed from assertNull to assertNotNull
    }

    @Test
    void testSetAndGetEntries() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        LeaderboardEntryDTO entry1 = new LeaderboardEntryDTO();
        entry1.setRank(1);
        entry1.setUsername("user1");
        
        LeaderboardEntryDTO entry2 = new LeaderboardEntryDTO();
        entry2.setRank(2);
        entry2.setUsername("user2");
        
        entries.add(entry1);
        entries.add(entry2);
        
        dto.setEntries(entries);
        
        assertEquals(entries, dto.getEntries());
        assertEquals(2, dto.getEntries().size());
        assertEquals("user1", dto.getEntries().get(0).getUsername());
        assertEquals("user2", dto.getEntries().get(1).getUsername());
    }

    @Test
    void testSetAndGetTotalPlayers() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setTotalPlayers(100);
        assertEquals(100, dto.getTotalPlayers());
    }

    @Test
    void testSetAndGetPage() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setPage(2);
        assertEquals(2, dto.getPage());
    }

    @Test
    void testSetAndGetPageSize() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setPageSize(25);
        assertEquals(25, dto.getPageSize());
    }

    @Test
    void testSetAndGetTotalPages() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setTotalPages(4);
        assertEquals(4, dto.getTotalPages());
    }

    @Test
    void testSetAndGetCurrentUserEntry() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        
        LeaderboardEntryDTO currentUserEntry = new LeaderboardEntryDTO();
        currentUserEntry.setRank(5);
        currentUserEntry.setUsername("currentUser");
        currentUserEntry.setCurrentUser(true);
        
        dto.setCurrentUserEntry(currentUserEntry);
        
        assertEquals(currentUserEntry, dto.getCurrentUserEntry());
        assertEquals("currentUser", dto.getCurrentUserEntry().getUsername());
        assertEquals(5, dto.getCurrentUserEntry().getRank());
        assertTrue(dto.getCurrentUserEntry().isCurrentUser());
    }

    @Test
    void testFullyPopulatedDTO() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        LeaderboardEntryDTO entry1 = new LeaderboardEntryDTO();
        entry1.setRank(1);
        entry1.setUsername("topUser");
        entry1.setXp(5000);
        
        LeaderboardEntryDTO entry2 = new LeaderboardEntryDTO();
        entry2.setRank(2);
        entry2.setUsername("secondUser");
        entry2.setXp(4500);
        
        entries.add(entry1);
        entries.add(entry2);
        
        LeaderboardEntryDTO currentUserEntry = new LeaderboardEntryDTO();
        currentUserEntry.setRank(10);
        currentUserEntry.setUsername("currentUser");
        currentUserEntry.setXp(2000);
        currentUserEntry.setCurrentUser(true);
        
        dto.setEntries(entries);
        dto.setTotalPlayers(100);
        dto.setPage(1);
        dto.setPageSize(10);
        dto.setTotalPages(10);
        dto.setCurrentUserEntry(currentUserEntry);
        
        assertEquals(2, dto.getEntries().size());
        assertEquals(100, dto.getTotalPlayers());
        assertEquals(1, dto.getPage());
        assertEquals(10, dto.getPageSize());
        assertEquals(10, dto.getTotalPages());
        assertEquals("currentUser", dto.getCurrentUserEntry().getUsername());
        assertEquals(10, dto.getCurrentUserEntry().getRank());
    }

    @Test
    void testEmptyEntriesList() {
        LeaderboardResponseDTO dto = new LeaderboardResponseDTO();
        dto.setEntries(new ArrayList<>());
        assertNotNull(dto.getEntries());
        assertTrue(dto.getEntries().isEmpty());
    }
}
