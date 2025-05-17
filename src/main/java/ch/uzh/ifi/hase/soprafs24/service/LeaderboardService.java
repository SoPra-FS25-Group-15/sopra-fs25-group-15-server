package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardResponseDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaderboardService {
    
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public LeaderboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Get the global leaderboard with pagination
     * 
     * @param page Page number (0-based)
     * @param pageSize Number of entries per page
     * @param currentUserId ID of the current user (to mark their entry)
     * @return Paginated leaderboard data
     */
    public LeaderboardResponseDTO getLeaderboard(int page, int pageSize, Long currentUserId) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be greater than or equal to 0");
        }
        if (pageSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be greater than 0");
        }

        // Log the input parameters
        log.debug("getLeaderboard called with page={}, pageSize={}, currentUserId={}", page, pageSize, currentUserId);

        // Get all users with their profiles
        List<User> allUsers = userRepository.findAll();
        
        // Filter out users without profiles and sort by XP
        List<User> sortedUsers = allUsers.stream()
            .filter(user -> user.getProfile() != null)
            .sorted(Comparator.<User, Integer>comparing(
                user -> user.getProfile().getXp(), 
                Comparator.reverseOrder()
            ))
            .collect(Collectors.toList());
        
        int totalPlayers = sortedUsers.size();
        int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);
        
        // Create new response DTO with clean state
        LeaderboardResponseDTO response = new LeaderboardResponseDTO();
        response.setTotalPlayers(totalPlayers);
        response.setPage(page);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);
        response.setCurrentUserEntry(null); // Explicitly set to null
        
        // Empty list of entries (required for out-of-bounds pages)
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        response.setEntries(entries);
        
        // Calculate page bounds
        int startIndex = page * pageSize;
        
        // Handle special case: if currentUserId is null, just add the page entries and return
        if (currentUserId == null) {
            log.debug("currentUserId is null, returning response without current user entry");
            
            // Only populate entries if page is valid
            if (startIndex < totalPlayers) {
                int endIndex = Math.min(startIndex + pageSize, totalPlayers);
                List<User> pageUsers = sortedUsers.subList(startIndex, endIndex);
                
                for (int i = 0; i < pageUsers.size(); i++) {
                    User user = pageUsers.get(i);
                    entries.add(convertToLeaderboardEntry(user, startIndex + i + 1));
                }
            }
            
            // IMPORTANT: Ensure currentUserEntry is null before returning
            response.setCurrentUserEntry(null);
            
            // Check that it's still null before returning
            if (response.getCurrentUserEntry() != null) {
                log.error("currentUserEntry is not null when it should be, forcing to null");
                response.setCurrentUserEntry(null);
            }
            
            return response;
        }
        
        // Everything below this point only executes if currentUserId is not null
        
        // Find current user's position in the full list
        int currentUserIndex = -1;
        for (int i = 0; i < sortedUsers.size(); i++) {
            if (sortedUsers.get(i).getId().equals(currentUserId)) {
                currentUserIndex = i;
                break;
            }
        }
        
        // Handle page out of bounds
        if (startIndex >= totalPlayers) {
            // If current user exists, add separate entry for them
            if (currentUserIndex != -1) {
                User currentUser = sortedUsers.get(currentUserIndex);
                LeaderboardEntryDTO currentUserEntry = convertToLeaderboardEntry(currentUser, currentUserIndex + 1);
                currentUserEntry.setCurrentUser(true);
                response.setCurrentUserEntry(currentUserEntry);
            }
            return response;
        }
        
        // Page is valid and currentUserId is provided
        int endIndex = Math.min(startIndex + pageSize, totalPlayers);
        List<User> pageUsers = sortedUsers.subList(startIndex, endIndex);
        
        boolean currentUserInPage = false;
        
        // Add all entries for this page
        for (int i = 0; i < pageUsers.size(); i++) {
            User user = pageUsers.get(i);
            LeaderboardEntryDTO entry = convertToLeaderboardEntry(user, startIndex + i + 1);
            
            // Check if this is the current user
            if (user.getId().equals(currentUserId)) {
                entry.setCurrentUser(true);
                currentUserInPage = true;
                // Also set this as the currentUserEntry for the response
                response.setCurrentUserEntry(entry);
            }
            
            entries.add(entry);
        }
        
        // Only add separate current user entry if not already in page
        if (!currentUserInPage && currentUserIndex != -1) {
            User currentUser = sortedUsers.get(currentUserIndex);
            LeaderboardEntryDTO currentUserEntry = convertToLeaderboardEntry(currentUser, currentUserIndex + 1);
            currentUserEntry.setCurrentUser(true);
            response.setCurrentUserEntry(currentUserEntry);
        }
        
        return response;
    }
    
    /**
     * Get the rank of a specific user
     * 
     * @param userId ID of the user
     * @return User's rank in the leaderboard
     */
    public int getUserRank(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no profile");
        }
        
        // Get all users with their profiles, sorted by XP
        List<User> sortedUsers = userRepository.findAll().stream()
            .filter(u -> u.getProfile() != null)
            .sorted(Comparator.<User, Integer>comparing(
                u -> u.getProfile().getXp(), 
                Comparator.reverseOrder()
            ))
            .collect(Collectors.toList());
        
        // Find the user's position
        for (int i = 0; i < sortedUsers.size(); i++) {
            if (sortedUsers.get(i).getId().equals(userId)) {
                return i + 1; // Ranks are 1-based
            }
        }
        
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in leaderboard");
    }
    
    /**
     * Get the nearby players in the leaderboard for a specific user
     * 
     * @param userId ID of the user
     * @param range Number of players to include above and below the user
     * @return Leaderboard entries around the user
     */
    public LeaderboardResponseDTO getUserLeaderboardRange(Long userId, int range) {
        if (range < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Range must be greater than or equal to 0");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getProfile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no profile");
        }
        
        // Get all users with their profiles, sorted by XP
        List<User> sortedUsers = userRepository.findAll().stream()
            .filter(u -> u.getProfile() != null)
            .sorted(Comparator.<User, Integer>comparing(
                u -> u.getProfile().getXp(), 
                Comparator.reverseOrder()
            ))
            .collect(Collectors.toList());
        
        // Find the user's position
        int userIndex = -1;
        for (int i = 0; i < sortedUsers.size(); i++) {
            if (sortedUsers.get(i).getId().equals(userId)) {
                userIndex = i;
                break;
            }
        }
        
        if (userIndex == -1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in leaderboard");
        }
        
        // Calculate the range of users to include
        int startIndex = Math.max(0, userIndex - range);
        int endIndex = Math.min(sortedUsers.size(), userIndex + range + 1);
        
        List<User> rangeUsers = sortedUsers.subList(startIndex, endIndex);
        
        // Convert to DTOs
        List<LeaderboardEntryDTO> entries = new ArrayList<>();
        for (int i = 0; i < rangeUsers.size(); i++) {
            User rangeUser = rangeUsers.get(i);
            LeaderboardEntryDTO entry = convertToLeaderboardEntry(rangeUser, startIndex + i + 1);
            
            // Mark if this is the target user
            if (rangeUser.getId().equals(userId)) {
                entry.setCurrentUser(true);
            }
            
            entries.add(entry);
        }
        
        // Create the response
        LeaderboardResponseDTO response = new LeaderboardResponseDTO();
        response.setEntries(entries);
        response.setTotalPlayers(sortedUsers.size());
        
        // We're not using pagination for range queries
        response.setPage(0);
        response.setPageSize(entries.size());
        response.setTotalPages(1);
        
        return response;
    }
    
    /**
     * Convert a User entity to a LeaderboardEntryDTO
     */
    private LeaderboardEntryDTO convertToLeaderboardEntry(User user, int rank) {
        UserProfile profile = user.getProfile();
        
        LeaderboardEntryDTO entry = new LeaderboardEntryDTO();
        entry.setRank(rank);
        entry.setUserId(user.getId());
        entry.setUsername(profile.getUsername());
        entry.setXp(profile.getXp());
        entry.setGamesPlayed(profile.getGamesPlayed());
        entry.setWins(profile.getWins());
        entry.setCurrentUser(false); // Default to false, will be set later if needed
        
        return entry;
    }
    
    /**
     * Find and add the current user's entry to the response
     * Note: This method is not currently being used
     */
    private void findAndAddCurrentUser(LeaderboardResponseDTO response, List<User> sortedUsers, Long currentUserId) {
        // Skip if currentUserId is null
        if (currentUserId == null) {
            return;
        }
        
        for (int i = 0; i < sortedUsers.size(); i++) {
            User user = sortedUsers.get(i);
            if (user.getId().equals(currentUserId)) {
                LeaderboardEntryDTO currentUserEntry = convertToLeaderboardEntry(user, i + 1);
                currentUserEntry.setCurrentUser(true);
                response.setCurrentUserEntry(currentUserEntry);
                break;
            }
        }
    }
}
