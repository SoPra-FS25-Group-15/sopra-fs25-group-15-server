package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing round cards in the game
 */
@Service
@Transactional
public class RoundCardService {
    private final Logger log = LoggerFactory.getLogger(RoundCardService.class);

    // Map of gameId -> playerId -> List<RoundCardDTO>
    private final Map<Long, Map<Long, List<RoundCardDTO>>> playerRoundCards = new ConcurrentHashMap<>();
    
    // NEW: Map of gameId -> userToken -> List<RoundCardDTO> for token-based auth
    private final Map<Long, Map<String, List<RoundCardDTO>>> tokenRoundCards = new ConcurrentHashMap<>();
    
    // NEW: Card cache to ensure consistency between lookup and assignment
    private final Map<String, RoundCardDTO> cardCache = new ConcurrentHashMap<>();

    private final Random random = new Random();

    // Round cards - only 2 cards as indicated
    private static final List<String> ROUND_CARDS = List.of(
        "7choices", "badsight"
    );

    @Autowired
    private AuthService authService;
    
    // Add GameService dependency with @Lazy to avoid circular dependency
    @Autowired
    @Lazy
    private GameService gameService;

    /**
     * Create a predefined round card
     */
    private RoundCardDTO createRoundCard(String id, String name, String description, int timeSeconds) {
        RoundCardDTO card = new RoundCardDTO();
        card.setId(id);
        card.setName(name);
        card.setDescription(description);

        // Create modifiers with time
        RoundCardModifiers modifiers = new RoundCardModifiers();
        modifiers.setGuessType("Precise");
        modifiers.setStreetView("Standard");
        modifiers.setTime(timeSeconds);

        card.setModifiers(modifiers);
        return card;
    }

    /**
     * Assign initial round cards to all players in the game
     */
    public void assignInitialRoundCards(Long gameId, List<Long> playerIds) {
        Map<Long, List<RoundCardDTO>> gameCards = new HashMap<>();

        // Initialize each player with standard round cards
        for (Long playerId : playerIds) {
            List<RoundCardDTO> cards = new ArrayList<>();

            // World card - 60 seconds
            cards.add(createRoundCard(
                "world-" + playerId + "-" + System.currentTimeMillis(),
                "World",
                "The round includes the full available coverage",
                60
            ));

            // Flash card - 30 seconds
            cards.add(createRoundCard(
                "flash-" + playerId + "-" + System.currentTimeMillis(),
                "Flash",
                "The round includes the full available coverage, but the round time is halved",
                30
            ));

            // Add more round cards as needed
            cards.add(createRoundCard(
                "standard-" + playerId + "-" + System.currentTimeMillis(),
                "Standard",
                "Standard round with normal timing",
                45
            ));

            // limit to exactly 2 cards per player
            if (cards.size() > 2) {
                cards = cards.subList(0, 2);
            }

            gameCards.put(playerId, cards);

            log.info("Assigned {} round cards to player {} in game {}", 
                    cards.size(), playerId, gameId);
        }

        playerRoundCards.put(gameId, gameCards);
    }

    /**
     * Get round cards for a specific player
     */
    public List<RoundCardDTO> getPlayerRoundCards(Long gameId, Long playerId) {
        Map<Long, List<RoundCardDTO>> gameCards = playerRoundCards.get(gameId);
        if (gameCards == null) {
            return new ArrayList<>();
        }

        List<RoundCardDTO> cards = gameCards.get(playerId);
        return cards != null ? cards : new ArrayList<>();
    }

    /**
     * Select a random player to start the game
     */
    public Long selectRandomPlayerToStart(List<Long> playerIds) {
        if (playerIds.isEmpty()) {
            throw new IllegalArgumentException("No players available");
        }
        return playerIds.get(random.nextInt(playerIds.size()));
    }

    /**
     * Remove a round card from a player's inventory with additional logging
     */
    public boolean removeRoundCardFromPlayer(Long gameId, Long playerId, String cardId) {
        Map<Long, List<RoundCardDTO>> inventories = playerRoundCards.computeIfAbsent(gameId, k -> new HashMap<>());
        List<RoundCardDTO> playerCards = inventories.get(playerId);
        
        if (playerCards == null) {
            log.warn("Cannot remove card {} - player {} has no inventory in game {}", cardId, playerId, gameId);
            return false;
        }
        
        // First log the cards the player has before removal
        log.info("Before removal - Player {} has {} cards: {}", 
                playerId, playerCards.size(), 
                playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        
        int originalSize = playerCards.size();
        playerCards.removeIf(card -> card.getId().equals(cardId));
        
        boolean removed = playerCards.size() < originalSize;
        
        // Log the updated inventory
        log.info("After removal - Player {} has {} cards: {}", 
                playerId, playerCards.size(), 
                playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        
        if (removed) {
            log.info("Successfully removed round card {} from player {} in game {}, remaining cards: {}", 
                    cardId, playerId, gameId, playerCards.size());
                    
            // Special check: if player has no cards left, log a warning
            if (playerCards.isEmpty()) {
                log.warn("GAME OVER: Player {} has NO ROUND CARDS LEFT after removing {} - game should end", 
                        playerId, cardId);
            }
        } else {
            log.warn("Failed to remove round card {} from player {} in game {} - card not found in inventory {}", 
                    cardId, playerId, gameId, 
                    playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        }
        
        return removed;
    }

    /**
     * Remove a round card from a player's inventory by token with additional logging
     * @param gameId ID of the game
     * @param playerToken Token of the player
     * @param cardId ID of the card to remove
     * @return true if the card was successfully removed
     */
    public boolean removeRoundCardFromPlayerByToken(Long gameId, String playerToken, String cardId) {
        // Check if player exists in the token-based map
        Map<String, List<RoundCardDTO>> gameTokenCards = tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        List<RoundCardDTO> playerCards = gameTokenCards.get(playerToken);
        
        if (playerCards == null) {
            log.warn("Cannot remove card {} - player token {} has no inventory in game {}", 
                    cardId, playerToken, gameId);
            return false;
        }
        
        // First log the cards the player has before removal
        log.info("Before removal - Player token {} has {} cards: {}", 
                playerToken, playerCards.size(), 
                playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        
        int originalSize = playerCards.size();
        playerCards.removeIf(card -> card.getId().equals(cardId));
        
        boolean removed = playerCards.size() < originalSize;
        
        // Log the updated inventory
        log.info("After removal - Player token {} has {} cards: {}", 
                playerToken, playerCards.size(), 
                playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        
        if (removed) {
            log.info("Successfully removed round card {} from player token {} in game {}, remaining cards: {}", 
                    cardId, playerToken, gameId, playerCards.size());
                    
            // Special check: if player has no cards left, log a warning
            if (playerCards.isEmpty()) {
                log.warn("Player token {} has NO ROUND CARDS LEFT after removing {} - they cannot select cards anymore", 
                        playerToken, cardId);
            }
        } else {
            log.warn("Failed to remove round card {} from player token {} in game {} - card not found in inventory {}", 
                    cardId, playerToken, gameId, 
                    playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        }
        
        return removed;
    }

    /**
     * Remove a round card from a player's inventory after it has been played
     * @param gameId ID of the game
     * @param playerToken Token of the player
     * @param cardId ID of the card to remove
     * @return true if the card was removed, false otherwise
     */
    public boolean removePlayerRoundCard(Long gameId, String playerToken, String cardId) {
        GameService.GameState gameState = gameService.getGameState(gameId);
        if (gameState == null) {
            log.error("Game not found: {}", gameId);
            return false;
        }
        
        // Get player's inventory
        GameService.GameState.PlayerInventory inventory = gameState.getInventoryForPlayer(playerToken);
        if (inventory == null) {
            log.error("Player inventory not found for token: {}", playerToken);
            return false;
        }
        
        // Get player info to update card counts
        GameService.GameState.PlayerInfo playerInfo = gameState.getPlayerInfo().get(playerToken);
        if (playerInfo == null) {
            log.error("Player info not found for token: {}", playerToken);
            return false;
        }
        
        // Remove the card from player's inventory
        boolean removed = inventory.getRoundCards().remove(cardId);
        if (removed) {
            log.info("Removed round card {} from player {}'s inventory", cardId, playerToken);
            
            // Update player's round card count
            int newCount = inventory.getRoundCards().size();
            playerInfo.setRoundCardsLeft(newCount);
            
            log.info("Player {} has {} round cards left", playerToken, newCount);
            return true;
        } else {
            log.warn("Could not find round card {} in player {}'s inventory", cardId, playerToken);
            return false;
        }
    }

    /**
     * Check if a player has no round cards left
     * @param gameId ID of the game
     * @param playerId ID of the player
     * @return true if the player has no round cards left
     */
    public boolean hasNoRoundCards(Long gameId, Long playerId) {
        List<RoundCardDTO> cards = getPlayerRoundCards(gameId, playerId);
        boolean noCards = cards.isEmpty();
        
        if (noCards) {
            log.info("Player {} has no round cards left in game {}", playerId, gameId);
        }
        
        return noCards;
    }

    /**
     * Check if a player has no round cards left by token
     * @param gameId ID of the game
     * @param playerToken Token of the player
     * @return true if the player has no round cards left
     */
    public boolean hasNoRoundCardsByToken(Long gameId, String playerToken) {
        Map<String, List<RoundCardDTO>> gameTokenCards = tokenRoundCards.get(gameId);
        if (gameTokenCards == null) return true;
        
        List<RoundCardDTO> cards = gameTokenCards.get(playerToken);
        boolean noCards = cards == null || cards.isEmpty();
        
        if (noCards) {
            log.info("Player token {} has no round cards left in game {}", playerToken, gameId);
        }
        
        return noCards;
    }

    /**
     * Get all available round cards as DTOs
     * @return List of all available round cards as DTOs
     */
    public List<RoundCardDTO> getAllRoundCards() {
        log.info("Getting all round cards as DTOs");
        List<RoundCardDTO> roundCardDTOs = new ArrayList<>();
        
        // Create a DTO for each card ID
        for (String cardId : ROUND_CARDS) {
            RoundCardDTO dto = new RoundCardDTO();
            dto.setId(cardId);
            
            // Set card name based on ID
            if ("7choices".equals(cardId)) {
                dto.setName("7 Choices");
                dto.setDescription("Reveal the continent of the target location.");
                
                RoundCardModifiers modifiers = new RoundCardModifiers();
                modifiers.setTime(30); // default round time
                dto.setModifiers(modifiers);
            }
            else if ("badsight".equals(cardId)) {
                dto.setName("Bad Sight");
                dto.setDescription("A player of your choice has their screen blurred for the first 15 seconds of the round.");
                
                RoundCardModifiers modifiers = new RoundCardModifiers();
                modifiers.setTime(30); // default round time
                dto.setModifiers(modifiers);
            }
            
            roundCardDTOs.add(dto);
        }
        
        return roundCardDTOs;
    }

    /**
     * Get all available round card IDs
     * @return List of all available round card IDs
     */
    public List<String> getRoundCardIds() {
        log.info("Getting all round card IDs");
        return new ArrayList<>(ROUND_CARDS);
    }

    /**
     * Assign round cards to a player using token
     * @param userToken Token of the user
     * @return List of round card DTOs assigned to the player
     */
    public List<RoundCardDTO> assignRoundCardsToPlayer(String userToken) {
        User user = authService.getUserByToken(userToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid user token: " + userToken);
        }
        
        List<RoundCardDTO> cards = new ArrayList<>();

        // Generate deterministic card IDs that will be consistent for this user
        // instead of using timestamps which change each time
        String tokenPrefix = userToken.substring(0, 8);
        
        // World card - 60 seconds
        String worldCardId = "world-" + tokenPrefix;
        RoundCardDTO worldCard = createRoundCard(
            worldCardId,
            "World",
            "The round includes the full available coverage",
            60
        );
        cards.add(worldCard);
        
        // Also add to cache for consistent lookup
        cardCache.put(worldCardId, worldCard);

        // Flash card - 30 seconds
        String flashCardId = "flash-" + tokenPrefix;
        RoundCardDTO flashCard = createRoundCard(
            flashCardId,
            "Flash",
            "The round includes the full available coverage, but the round time is halved",
            30
        );
        cards.add(flashCard);
        
        // Also add to cache for consistent lookup
        cardCache.put(flashCardId, flashCard);

        // Limit to exactly 2 cards (in case we added more)
        if (cards.size() > 2) {
            cards = cards.subList(0, 2);
        }

        log.info("Assigned {} round cards to player with token {}: {}, {}", 
                cards.size(), userToken, worldCardId, flashCardId);
        
        return cards;
    }

    /**
     * Assign round cards to a player for a specific game
     * @param gameId ID of the game
     * @param userToken Token of the user
     * @return List of round card DTOs assigned to the player
     */
    public List<RoundCardDTO> assignPlayerRoundCards(Long gameId, String userToken) {
        // First generate the cards
        List<RoundCardDTO> cards = assignRoundCardsToPlayer(userToken);
        
        // Then store them in the game-specific map
        Map<String, List<RoundCardDTO>> gameCards = tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        gameCards.put(userToken, new ArrayList<>(cards));
        
        // Log full card details for easy debugging
        log.info("Stored {} round cards for game {} and token {}: {}", 
                cards.size(), gameId, userToken, 
                cards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        
        return cards;
    }

    /**
     * Get the round cards for a player using token
     * @param gameId ID of the game
     * @param userToken Token of the user
     * @return List of round card DTOs for the player
     */
    public List<RoundCardDTO> getPlayerRoundCardsByToken(Long gameId, String userToken) {
        User user = authService.getUserByToken(userToken);
        if (user == null) {
            log.error("Invalid user token: {}", userToken);
            throw new IllegalArgumentException("Invalid user token");
        }
        
        // Check if we have already assigned cards for this token
        Map<String, List<RoundCardDTO>> gameTokenCards = tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        List<RoundCardDTO> cards = gameTokenCards.get(userToken);
        
        // If no cards found, check if we have ID-based cards for this user and migrate them
        if (cards == null || cards.isEmpty()) {
            log.warn("No cards found for token {} in game {}, generating new ones", 
                    userToken, gameId);
            cards = assignPlayerRoundCards(gameId, userToken);
        }
        
        if (cards != null) {
            log.info("Found {} cards for token {} in game {}: {}", 
                    cards.size(), userToken, gameId,
                    cards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(", ")));
        } else {
            log.warn("Still no cards found for token {} in game {} after all attempts", userToken, gameId);
            // Return empty list instead of null
            return new ArrayList<>();
        }
        
        return cards;
    }
}
