package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerXpUpdateDTO;

@Service
@Transactional
public class UserXpService {
    
    private static final Logger log = LoggerFactory.getLogger(UserXpService.class);
    
    // XP Constants
    public static final int XP_FOR_GUESS = 10;
    public static final int XP_FOR_ROUND_WIN = 20;
    public static final int XP_FOR_GAME_WIN = 50;
    
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public UserXpService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Award XP to a user for making a guess
     * @param user The user who made the guess
     */
    public void awardXpForGuess(User user) {
        awardXp(user, XP_FOR_GUESS, "Made a guess");
    }
    
    /**
     * Award XP to a user for winning a round
     * @param user The user who won the round
     */
    public void awardXpForRoundWin(User user) {
        awardXp(user, XP_FOR_ROUND_WIN, "Won a round");
    }
    
    /**
     * Award XP to a user for winning a game
     * @param user The user who won the game
     */
    public void awardXpForGameWin(User user) {
        awardXp(user, XP_FOR_GAME_WIN, "Won a game");
    }
    
    /**
     * Award XP to a user and send a WebSocket notification
     * @param user The user to award XP to
     * @param xpAmount The amount of XP to award
     * @param reason The reason for the XP award
     */
    @Transactional
    public void awardXp(User user, int xpAmount, String reason) {
        if (user == null) {
            log.warn("Attempted to award XP to null user");
            return;
        }
        
        if (user.getToken() == null) {
            log.warn("User {} has no token, cannot send XP notification", user.getId());
            return;
        }
        
        int currentXp = user.getProfile().getXp();
        int newXp = currentXp + xpAmount;
        
        // Update XP in database
        user.getProfile().setXp(newXp);
        userRepository.save(user);
        
        log.info("Awarded {} XP to user {} ({}). New total: {}", 
            xpAmount, user.getId(), user.getProfile().getUsername(), newXp);
        
        // Send WebSocket notification
        PlayerXpUpdateDTO xpUpdateDTO = new PlayerXpUpdateDTO(
            user.getProfile().getUsername(),
            user.getId(),
            xpAmount,
            newXp,
            reason
        );
        
        // Send to the specific user
        messagingTemplate.convertAndSendToUser(
            user.getToken(),
            "/queue/xp-updates",
            xpUpdateDTO
        );
    }
    
    /**
     * Sends an XP update notification to a user via WebSocket
     */
    public void sendXpUpdateNotification(User user, int xpGained, int totalXp, String reason) {
        if (user == null || user.getToken() == null) {
            log.warn("Cannot send XP notification to user with null token");
            return;
        }
        
        try {
            PlayerXpUpdateDTO xpUpdateDTO = new PlayerXpUpdateDTO(
                user.getProfile().getUsername(),
                user.getId(),
                xpGained,
                totalXp,
                reason
            );
            
            // Send to the specific user
            messagingTemplate.convertAndSendToUser(
                user.getToken(),
                "/queue/xp-updates",
                xpUpdateDTO
            );
            
            log.debug("Sent XP update notification to user {}: +{} XP", 
                user.getId(), xpGained);
        } catch (Exception e) {
            log.error("Failed to send XP update notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get current XP for a user and send a notification
     * This can be called when a user requests their current XP
     */
    public void sendCurrentXpNotification(User user) {
        if (user == null) {
            log.warn("Cannot send current XP notification to null user");
            return;
        }
        
        int currentXp = user.getProfile().getXp();
        sendXpUpdateNotification(user, 0, currentXp, "XP status request");
    }
}
