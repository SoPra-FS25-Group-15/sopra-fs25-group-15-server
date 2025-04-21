package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class GameRoundService {

    private final Logger log = LoggerFactory.getLogger(GameRoundService.class);
    
    private final ActionCardService actionCardService;
    private final GameService gameService;
    private final GoogleMapsService googleMapsService;
    private final AuthService authService;
    private final ActionCardMapper actionCardMapper;
    
    // Track current game rounds
    private final Map<Long, Integer> gameRounds = new ConcurrentHashMap<>();
    
    // Track player action cards by token instead of ID
    private final Map<Long, Map<String, ActionCardDTO>> playerActionCards = new ConcurrentHashMap<>();

    @Autowired
    public GameRoundService(ActionCardService actionCardService, 
                          @Lazy GameService gameService, 
                          GoogleMapsService googleMapsService, 
                          AuthService authService,
                          ActionCardMapper actionCardMapper) {
        this.actionCardService = actionCardService;
        this.gameService = gameService;
        this.googleMapsService = googleMapsService;
        this.authService = authService;
        this.actionCardMapper = actionCardMapper;
    }
    
    /**
     * Start a new game with the specified players
     */
    public void startGame(Long gameId, List<String> playerTokens) {
        gameRounds.put(gameId, 0);
        log.info("Started new game for lobby {} with {} players", gameId, playerTokens.size());
    }
    
    /**
     * Start the next round for a game
     */
    public RoundData startNextRound(Long gameId, List<String> playerTokens) {
        int currentRound = gameRounds.getOrDefault(gameId, 0) + 1;
        gameRounds.put(gameId, currentRound);
        
        // Get coordinates from GoogleMapsService
        LatLngDTO coordinates = googleMapsService.getRandomCoordinatesOnLand();
        
        return new RoundData(currentRound, coordinates.getLatitude(), coordinates.getLongitude(), 1);
    }
    
    /**
     * Check if there are more rounds for the game
     */
    public boolean hasMoreRounds(Long gameId) {
        int currentRound = gameRounds.getOrDefault(gameId, 0);
        return currentRound < 3; // Max 3 rounds per game
    }
    
    /**
     * Distribute fresh action cards to all players using tokens
     * @param gameId ID of the game
     * @param playerTokens List of player tokens
     * @return Map of player tokens to their action cards
     */
    public Map<String, ActionCardDTO> distributeFreshActionCardsByToken(Long gameId, List<String> playerTokens) {
        Map<String, ActionCardDTO> tokenToActionCard = new HashMap<>();
        Map<String, ActionCardDTO> gamePlayerCards = playerActionCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        
        // Get game state for updating
        GameService.GameState gameState = gameService.getGameState(gameId);
        
        for (String token : playerTokens) {
            try {
                User user = authService.getUserByToken(token);
                if (user != null) {
                    // Draw a random action card for each player
                    ActionCardDTO card = actionCardService.drawRandomCard();
                    gamePlayerCards.put(token, card);
                    tokenToActionCard.put(token, card);
                    
                    // Update the game state with the action card
                    if (gameState != null) {
                        GameService.GameState.PlayerInventory inventory = gameState.getInventoryForPlayer(token);
                        List<String> actionCards = new ArrayList<>();
                        actionCards.add(card.getId()); // Add the card ID
                        inventory.setActionCards(actionCards);
                        
                        // Update player info
                        GameService.GameState.PlayerInfo playerInfo = gameState.getPlayerInfo().get(token);
                        if (playerInfo != null) {
                            playerInfo.setActionCardsLeft(1);
                        }
                    }
                    
                    log.info("Assigned action card {} to player with token {} in game {}", card.getId(), token, gameId);
                } else {
                    log.warn("Could not find user for token: {}", token);
                }
            } catch (Exception e) {
                log.error("Error distributing action card to player with token {}: {}", token, e.getMessage());
            }
        }
        
        return tokenToActionCard;
    }
    
    /**
     * Replace a player's action card using token
     * @param gameId ID of the game
     * @param userToken Token of the user
     * @return The new action card DTO
     */
    public ActionCardDTO replacePlayerActionCardByToken(Long gameId, String userToken) {
        Map<String, ActionCardDTO> gamePlayerCards = playerActionCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        
        // Draw a new action card
        ActionCardDTO newCard = actionCardService.drawRandomCard();
        gamePlayerCards.put(userToken, newCard);
        
        // Update game state
        GameService.GameState gameState = gameService.getGameState(gameId);
        if (gameState != null) {
            GameService.GameState.PlayerInventory inventory = gameState.getInventoryForPlayer(userToken);
            List<String> actionCards = new ArrayList<>();
            actionCards.add(newCard.getId());
            inventory.setActionCards(actionCards);
        }
        
        return newCard;
    }
    
    /**
     * Helper class for round data
     */
    public static class RoundData {
        private final int roundNumber;
        private final double latitude;
        private final double longitude;
        private final int guesses;
        
        public RoundData(int roundNumber, double latitude, double longitude, int guesses) {
            this.roundNumber = roundNumber;
            this.latitude = latitude;
            this.longitude = longitude;
            this.guesses = guesses;
        }
        
        public int getRoundNumber() {
            return roundNumber;
        }
        
        public double getLatitude() {
            return latitude;
        }
        
        public double getLongitude() {
            return longitude;
        }
        
        public int getGuesses() {
            return guesses;
        }
    }
}
