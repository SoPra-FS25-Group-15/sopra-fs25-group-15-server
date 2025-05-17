package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardResponseDTO;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for leaderboard-related endpoints
 */
@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {
    
    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);
    
    private final LeaderboardService leaderboardService;
    private final AuthService authService;
    
    @Autowired
    public LeaderboardController(LeaderboardService leaderboardService, AuthService authService) {
        this.leaderboardService = leaderboardService;
        this.authService = authService;
    }
    
    /**
     * Get the global leaderboard with pagination
     * 
     * @param token Authentication token
     * @param page Page number (0-based)
     * @param pageSize Number of entries per page
     * @return Paginated leaderboard data
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public LeaderboardResponseDTO getLeaderboard(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int pageSize) {
        
        log.info("Getting leaderboard page {} with size {}", page, pageSize);
        
        // Authenticate the user
        User currentUser = authService.getUserByToken(token);
        
        // Get the leaderboard data
        return leaderboardService.getLeaderboard(page, pageSize, currentUser.getId());
    }
    
    /**
     * Get a user's rank in the leaderboard
     * 
     * @param token Authentication token
     * @param userId ID of the user
     * @return User's rank
     */
    @GetMapping("/users/{userId}/rank")
    @ResponseStatus(HttpStatus.OK)
    public int getUserRank(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        
        log.info("Getting rank for user ID {}", userId);
        
        // Authenticate the user
        authService.getUserByToken(token);
        
        // Get the user's rank
        return leaderboardService.getUserRank(userId);
    }
    
    /**
     * Get the leaderboard entries around a specific user
     * 
     * @param token Authentication token
     * @param userId ID of the user
     * @param range Number of entries to include above and below the user
     * @return Leaderboard entries around the user
     */
    @GetMapping("/users/{userId}/range")
    @ResponseStatus(HttpStatus.OK)
    public LeaderboardResponseDTO getUserLeaderboardRange(
            @RequestHeader("Authorization") String token,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2") int range) {
        
        log.info("Getting leaderboard range for user ID {} with range {}", userId, range);
        
        // Authenticate the user
        authService.getUserByToken(token);
        
        // Get the leaderboard range
        return leaderboardService.getUserLeaderboardRange(userId, range);
    }
    
    /**
     * Get the top players in the leaderboard
     * 
     * @param token Authentication token
     * @param count Number of top players to retrieve
     * @return Leaderboard with top players
     */
    @GetMapping("/top")
    @ResponseStatus(HttpStatus.OK)
    public LeaderboardResponseDTO getTopPlayers(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "2") int count) {
        
        log.info("Getting top {} players", count);
        
        // Authenticate the user
        User currentUser = authService.getUserByToken(token);
        
        // Get the top players (equivalent to page 0 with pageSize = count)
        return leaderboardService.getLeaderboard(0, count, currentUser.getId());
    }
}
