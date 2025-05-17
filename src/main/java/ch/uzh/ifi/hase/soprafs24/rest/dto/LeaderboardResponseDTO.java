package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

/**
 * DTO for the complete leaderboard response, including pagination info
 */
public class LeaderboardResponseDTO {
    
    private List<LeaderboardEntryDTO> entries;
    private int totalPlayers;
    private int page;
    private int pageSize;
    private int totalPages;
    
    // Optional fields for the current user's position
    private LeaderboardEntryDTO currentUserEntry = new LeaderboardEntryDTO(); // Ensure not null by default
    
    public List<LeaderboardEntryDTO> getEntries() {
        return entries;
    }
    
    public void setEntries(List<LeaderboardEntryDTO> entries) {
        this.entries = entries;
    }
    
    public int getTotalPlayers() {
        return totalPlayers;
    }
    
    public void setTotalPlayers(int totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
    
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public LeaderboardEntryDTO getCurrentUserEntry() {
        return currentUserEntry;
    }
    
    public void setCurrentUserEntry(LeaderboardEntryDTO currentUserEntry) {
        this.currentUserEntry = currentUserEntry != null ? currentUserEntry : new LeaderboardEntryDTO();
    }
}
