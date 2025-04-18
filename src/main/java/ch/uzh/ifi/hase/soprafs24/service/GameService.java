package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to the game
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class GameService {

    private final Logger log = LoggerFactory.getLogger(GameService.class);

    // Track played cards in the current round
    private final Map<Long, Set<Long>> cardsPlayedInRound = new HashMap<>();
    
    // Track punishments in the current round (gameId -> targetPlayerId -> cardIds)
    private final Map<Long, Map<Long, Set<String>>> punishmentsInRound = new HashMap<>();
    
    // Track card plays with details (gameId -> userId -> (cardId, targetPlayerId))
    private final Map<Long, Map<Long, Map<String, Long>>> cardPlayDetails = new HashMap<>();

    // Constructor
    @Autowired
    public GameService() {
        // Initialize any dependencies
    }
    
    /**
     * Checks if a player has already played a card in the current round
     * @param gameId ID of the game
     * @param userId ID of the user
     * @return true if the player has already played a card in this round
     */
    public boolean isCardPlayedInCurrentRound(Long gameId, Long userId) {
        if (!cardsPlayedInRound.containsKey(gameId)) {
            return false;
        }
        return cardsPlayedInRound.get(gameId).contains(userId);
    }
    
    /**
     * Checks if a player has been punished this round with a specific card
     * @param gameId ID of the game
     * @param targetPlayerId ID of the targeted player
     * @param cardId ID of the card used for punishment
     * @return true if the player has been punished with this card
     */
    public boolean isPlayerPunishedThisRound(Long gameId, Long targetPlayerId, String cardId) {
        if (!punishmentsInRound.containsKey(gameId)) {
            return false;
        }
        
        Map<Long, Set<String>> gamePunishments = punishmentsInRound.get(gameId);
        if (!gamePunishments.containsKey(targetPlayerId)) {
            return false;
        }
        
        return gamePunishments.get(targetPlayerId).contains(cardId);
    }
    
    /**
     * Marks that a player has played a card this round
     * @param gameId ID of the game
     * @param userId ID of the user who played the card
     * @param cardId ID of the played card
     * @param targetPlayerId ID of the target player (if applicable)
     */
    public void markCardPlayedThisRound(Long gameId, Long userId, String cardId, Long targetPlayerId) {
        // Record that user played a card this round
        cardsPlayedInRound.computeIfAbsent(gameId, k -> new HashSet<>()).add(userId);
        
        // Record card play details
        Map<Long, Map<String, Long>> gameCardPlays = cardPlayDetails.computeIfAbsent(gameId, k -> new HashMap<>());
        Map<String, Long> userCardPlays = gameCardPlays.computeIfAbsent(userId, k -> new HashMap<>());
        userCardPlays.put(cardId, targetPlayerId);
        
        // If targeting another player for punishment, record it
        if (targetPlayerId != null) {
            Map<Long, Set<String>> gamePunishments = punishmentsInRound.computeIfAbsent(gameId, k -> new HashMap<>());
            gamePunishments.computeIfAbsent(targetPlayerId, k -> new HashSet<>()).add(cardId);
        }
        
        log.info("Player {} played card {} targeting player {} in game {}", userId, cardId, targetPlayerId, gameId);
    }
    
    /**
     * Resets the cards played tracking for a new round
     * @param gameId ID of the game to reset
     */
    public void resetRoundTracking(Long gameId) {
        cardsPlayedInRound.remove(gameId);
        punishmentsInRound.remove(gameId);
        cardPlayDetails.remove(gameId);
        log.info("Reset round tracking for game {}", gameId);
    }

    /**
     * Resets the card played state for a specific user in the current round
     * @param gameId ID of the game
     * @param userId ID of the user
     */
    public void resetCardPlayedThisRound(Long gameId, Long userId) {
        // TODO implement resetting logic
    }

    // Add other game-related methods as needed
}
