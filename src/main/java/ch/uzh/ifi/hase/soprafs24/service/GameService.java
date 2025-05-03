package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameWinnerBroadcast;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RoundWinnerBroadcast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Game Service
 * This class is the "worker" and responsible for all functionality related to the game
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class GameService {

    private final Logger log = LoggerFactory.getLogger(GameService.class);
    
    private final GoogleMapsService googleMapsService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRoundService gameRoundService;
    private final AuthService authService; // Add AuthService as a dependency

    @Autowired
    private RoundCardService roundCardService;

    // Track played cards in the current round
    private final Map<Long, Set<String>> cardsPlayedInRound = new HashMap<>();
    
    // Track punishments in the current round (gameId -> targetPlayerToken -> cardIds)
    private final Map<Long, Map<String, Set<String>>> punishmentsInRound = new HashMap<>();
    
    // Track card plays with details (gameId -> userToken -> (cardId, targetPlayerToken))
    private final Map<Long, Map<String, Map<String, String>>> cardPlayDetails = new HashMap<>();
    
    // Track the current player turn and round status
    private final Map<Long, GameState> gameStates = new ConcurrentHashMap<>();

    // Constructor
    @Autowired
    public GameService(GoogleMapsService googleMapsService, 
                       SimpMessagingTemplate messagingTemplate,
                       @Lazy GameRoundService gameRoundService,
                       AuthService authService) { // Add AuthService to constructor
        this.googleMapsService = googleMapsService;
        this.messagingTemplate = messagingTemplate;
        this.gameRoundService = gameRoundService;
        this.authService = authService; // Initialize AuthService
    }
    
    /**
     * Initialize a new game
     * @param gameId ID of the game
     * @param playerTokens List of player tokens
     * @param startingPlayerToken Token of the player who starts the first round
     */
    public void initializeGame(Long gameId, List<String> playerTokens, String startingPlayerToken) {
        GameState gameState = new GameState();
        gameState.setPlayerTokens(playerTokens);
        
        // FIXED: Always respect the provided starting player token if it's valid
        if (startingPlayerToken != null && playerTokens.contains(startingPlayerToken)) {
            gameState.setCurrentTurnPlayerToken(startingPlayerToken);
            log.info("Using provided starting player token: {}", startingPlayerToken);
        } else {
            // Only select random if no valid token provided
            startingPlayerToken = selectRandomPlayerWithCards(gameId, playerTokens);
            gameState.setCurrentTurnPlayerToken(startingPlayerToken);
            log.info("Selected random starting player token: {}", startingPlayerToken);
        }
        
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
        // initialize round counter to 0
        gameState.setCurrentRound(0);

        // Assign round cards to players through RoundCardService
        for (String playerToken : playerTokens) {
            List<RoundCardDTO> cards = roundCardService.assignPlayerRoundCards(gameId, playerToken);
            List<String> cardIds = new ArrayList<>();
            for (RoundCardDTO card : cards) {
                cardIds.add(card.getId());
            }
            gameState.getInventoryForPlayer(playerToken).setRoundCards(cardIds);
            log.info("Initialized player {} with {} round cards in game {}", playerToken, cards.size(), gameId);
        }

        gameStates.put(gameId, gameState);
        log.info("Initialized game {} with {} players, starting player token: {}",
                 gameId, playerTokens.size(), startingPlayerToken);
    }
    
    /**
     * Check if a player has round cards left
     * @param gameId ID of the game
     * @param playerToken Token of the player to check
     * @return true if player has round cards, false otherwise
     */
    public boolean hasRoundCards(Long gameId, String playerToken) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) return false;
        
        GameState.PlayerInventory inventory = gameState.getInventoryForPlayer(playerToken);
        return inventory != null && inventory.getRoundCards() != null && !inventory.getRoundCards().isEmpty();
    }
    
    /**
     * Select a random player who still has round cards
     * @param gameId ID of the game
     * @param playerTokens List of all player tokens
     * @return Token of a player with round cards, or first player if none have cards
     */
    public String selectRandomPlayerWithCards(Long gameId, List<String> playerTokens) {
        if (playerTokens == null || playerTokens.isEmpty()) {
            return null;
        }
        
        // First try to find players with cards
        List<String> playersWithCards = playerTokens.stream()
            .filter(token -> hasRoundCards(gameId, token))
            .collect(Collectors.toList());
        
        if (!playersWithCards.isEmpty()) {
            // Select a random player from those who have cards
            return playersWithCards.get(new Random().nextInt(playersWithCards.size()));
        }
        
        // If nobody has cards (should be rare), just pick someone randomly
        return playerTokens.get(new Random().nextInt(playerTokens.size()));
    }
    
    /**
     * Get the current game state
     * @param gameId ID of the game
     * @return the current game state
     */
    public GameState getGameState(Long gameId) {
        return gameStates.get(gameId);
    }
    
    /**
     * Start a new round with the selected round card
     * @param gameId ID of the game
     * @param roundCard the selected round card for this round
     * @return coordinates for the round
     */
    public LatLngDTO startRound(Long gameId, RoundCardDTO roundCard) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.error("Game not initialized when starting round: {}", gameId);
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        String currentTurnPlayerToken = gameState.getCurrentTurnPlayerToken();
        log.info("Starting round for game {}, current turn player token: {}, status: {}", 
                gameId, currentTurnPlayerToken, gameState.getStatus());
        
        // Allow starting round in more states to avoid blockages
        if (gameState.getStatus() != GameStatus.WAITING_FOR_ROUND_CARD) {
            log.warn("Starting round while in state {} - proceeding anyway", gameState.getStatus());
        }
        
        // FIXED: Check if the round card is already active - only if activeRoundCard and cardID match
        if (roundCard.getId().equals(gameState.getActiveRoundCard()) && gameState.getCurrentLatLngDTO() != null) {
            log.warn("Round card {} is already active, not reprocessing", roundCard.getId());
            return gameState.getCurrentLatLngDTO();
        }
        
        // Always increment round counter when starting a new round
        gameState.setCurrentRound(gameState.getCurrentRound() + 1);
        log.info("Incremented round counter to {} for new round", gameState.getCurrentRound());
        
        // Fetch random coordinates from Google Maps API with retry mechanism
        LatLngDTO coordinates = null;
        int maxRetries = 3;
        int retryCount = 0;
        
        while (coordinates == null && retryCount < maxRetries) {
            try {
                coordinates = googleMapsService.getRandomCoordinatesOnLand(gameId);
                log.info("Generated random coordinates for game {}: {}, {}", 
                        gameId, coordinates.getLatitude(), coordinates.getLongitude());
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("Failed to fetch coordinates after {} attempts: {}", maxRetries, e.getMessage());
                    throw new IllegalStateException("Failed to fetch coordinates: " + e.getMessage());
                }
                log.warn("Attempt {} failed to fetch coordinates, retrying...", retryCount);
                // Short delay before retry
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (coordinates == null) {
            // Use fallback coordinates if all retries fail (Paris, France)
            log.warn("Using fallback coordinates after failed retries");
            coordinates = new LatLngDTO(48.8566, 2.3522); 
        }
        
        // Update game state - CRITICAL: Store the round card ID as activeRoundCard
        gameState.setCurrentRoundCard(roundCard);
        gameState.setActiveRoundCard(roundCard.getId());
        log.info("Set active round card to: {}", roundCard.getId());
        
        // Store coordinates directly as LatLngDTO
        gameState.setCurrentLatLngDTO(coordinates);
        gameState.setStatus(GameStatus.WAITING_FOR_ACTION_CARDS);
        
        // Update screen
        gameState.setCurrentScreen("ACTIONCARD");
        
        // Store coordinates in guessScreenAttributes - CRITICAL: Make sure these are set
        gameState.getGuessScreenAttributes().setLatitude(coordinates.getLatitude());
        gameState.getGuessScreenAttributes().setLongitude(coordinates.getLongitude());
        log.info("Updated guessScreenAttributes with lat: {}, lon: {}", 
                coordinates.getLatitude(), coordinates.getLongitude());
        
        // Set round timer based on the round card modifiers
        if (roundCard.getModifiers() != null && roundCard.getModifiers().getTime() > 0) {
            gameState.getGuessScreenAttributes().setTime(roundCard.getModifiers().getTime());
            log.info("Set round time to {}s from round card {}", 
                    roundCard.getModifiers().getTime(), roundCard.getId());
        } else {
            // Default time if no modifiers present
            gameState.getGuessScreenAttributes().setTime(30);
            log.info("Using default round time (30s) for card {} since no modifiers found",
                    roundCard.getId());
        }
        
        log.info("Started round {} in game {} with round card {}, coordinates: {}, {}, time: {}s", 
                gameState.getCurrentRound(), gameId, roundCard.getId(), 
                coordinates.getLatitude(), coordinates.getLongitude(),
                gameState.getGuessScreenAttributes().getTime());
                
        // Reset action cards played for the new round
        resetRoundTracking(gameId);
                
        return coordinates;
    }
    
    /**
     * Add action card effect to player
     * @param gameId ID of the game
     * @param targetPlayerToken Token of the targeted player
     * @param actionCardId ID of the action card being applied
     * @return true if successfully applied
     */
    public boolean applyActionCardToPlayer(Long gameId, String targetPlayerToken, String actionCardId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) return false;
        
        GameState.PlayerInfo info = gameState.getPlayerInfo().get(targetPlayerToken);
        if (info == null) {
            info = new GameState.PlayerInfo();
            gameState.getPlayerInfo().put(targetPlayerToken, info);
        }
        
        // Add action card ID to player's active effects if not already there
        if (info.getActiveActionCards() == null) {
            info.setActiveActionCards(new ArrayList<>());
        }
        
        if (!info.getActiveActionCards().contains(actionCardId)) {
            info.getActiveActionCards().add(actionCardId);
            log.info("Applied action card {} to player {}", actionCardId, targetPlayerToken);
            return true;
        }
        
        return false;
    }
    
    /**
     * Process all action cards played and start the guessing phase
     * @param gameId ID of the game
     */
    public void startGuessingPhase(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        // Make this method more flexible by accepting more valid states
        if (gameState.getStatus() != GameStatus.WAITING_FOR_ACTION_CARDS && 
            gameState.getStatus() != GameStatus.WAITING_FOR_ROUND_CARD) {
            // Only throw exception if we're already in guessing or later phase
            if (gameState.getStatus() == GameStatus.WAITING_FOR_GUESSES ||
                gameState.getStatus() == GameStatus.ROUND_COMPLETE ||
                gameState.getStatus() == GameStatus.GAME_OVER) {
                throw new IllegalStateException("Game is already in or past guessing phase");
            }
        }
        
        // Clear existing guesses for the new round
        gameState.getPlayerGuesses().clear();
        
        // We're either waiting for round card, action cards, or in an initial state
        // In any case, we can proceed to guessing phase
        gameState.setStatus(GameStatus.WAITING_FOR_GUESSES);
        gameState.setCurrentScreen("GUESS");
        
        // Ensure the guessScreenAttributes are properly set for the start of the round
        if (gameState.getCurrentLatLngDTO() != null) {
            double latitude = gameState.getCurrentLatLngDTO().getLatitude();
            double longitude = gameState.getCurrentLatLngDTO().getLongitude();
            
            // Make sure coordinates are properly set
            gameState.getGuessScreenAttributes().setLatitude(latitude);
            gameState.getGuessScreenAttributes().setLongitude(longitude);
            log.info("Set guessing phase coordinates in guessScreenAttributes: {}, {}", latitude, longitude);
        } else {
            log.warn("No coordinates available when starting guessing phase for game {}", gameId);
        }
        
        // Verify time is set - default to 30 seconds if not already specified
        if (gameState.getGuessScreenAttributes().getTime() <= 0) {
            gameState.getGuessScreenAttributes().setTime(30);
            log.info("Set default round time (30s) as none was previously specified");
        }
        
        log.info("Started guessing phase in game {} with round time {}s", 
                 gameId, gameState.getGuessScreenAttributes().getTime());
    }
    
    /**
     * Submit a player's guess
     * @param gameId ID of the game
     * @param playerToken Token of the player
     * @param lat latitude of the guess
     * @param lon longitude of the guess
     */
    public void submitGuess(Long gameId, String playerToken, double lat, double lon) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        if (gameState.getStatus() != GameStatus.WAITING_FOR_GUESSES) {
            throw new IllegalStateException("Game is not in guessing phase");
        }
        
        // Calculate distance from target LatLngDTO
        LatLngDTO targetCoords = gameState.getCurrentLatLngDTO();
        LatLngDTO guessCoords = new LatLngDTO(lat, lon);
        double distanceMeters = calculateDistance(targetCoords, guessCoords);
        
        // Store distance as integer (rounded to nearest meter)
        int distanceInt = (int) Math.round(distanceMeters);
        
        // FIXED: Add synchronized block to prevent race conditions when storing guesses
        synchronized (gameState.getPlayerGuesses()) {
            gameState.getPlayerGuesses().put(playerToken, distanceInt);
            
            // Log detailed guess submission stats
            log.info("Player guess for token {} registered. Current guesses: {}/{}, tokens with guesses: {}", 
                playerToken, gameState.getPlayerGuesses().size(), gameState.getPlayerTokens().size(),
                String.join(", ", gameState.getPlayerGuesses().keySet()));
        }
        
        log.info("Player with token {} submitted guess in game {}: {}, {} (distance: {}m)", 
                playerToken, gameId, lat, lon, distanceInt);
    }
    
    /**
     * Register a guess from a player and check if the round/game is complete
     * @param gameId ID of the game
     * @param playerToken Token of the player making the guess
     * @param latitude Latitude of the guess
     * @param longitude Longitude of the guess
     */
    public void registerGuess(Long gameId, String playerToken, double latitude, double longitude) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        // Submit the guess
        submitGuess(gameId, playerToken, latitude, longitude);
        
        // Broadcast guess to all players
        String username = gameState.getPlayerInfo().get(playerToken).getUsername();
        
        // Calculate distance
        LatLngDTO targetCoords = gameState.getCurrentLatLngDTO();
        LatLngDTO guessCoords = new LatLngDTO(latitude, longitude);
        int distanceMeters = (int) Math.round(calculateDistance(targetCoords, guessCoords));
        
        // Broadcast the guess
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + gameId + "/game",
            new WebSocketMessage<>("GUESS_SUBMITTED", Map.of(
                "username", username,
                "latitude", latitude,
                "longitude", longitude,
                "distance", distanceMeters,
                "round", gameState.getCurrentRound()
            ))
        );
        
        log.info("Player with token {} ({}) registered guess in game {} with distance {}m",
                playerToken, username, gameId, distanceMeters);
        
        // If all players have submitted guesses, automatically determine winner
        if (areAllGuessesSubmitted(gameId)) {
            String winnerToken = determineRoundWinner(gameId);
            String winnerUsername = gameState.getPlayerInfo().get(winnerToken).getUsername();
            int winningDistance = gameState.getPlayerGuesses().get(winnerToken);
            
            // Broadcast round winner with structured message
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + gameId + "/game",
                new RoundWinnerBroadcast(winnerUsername, gameState.getCurrentRound(), winningDistance)
            );
            
            // Check if this was the final round
            // Game ends when current round reaches the max rounds value
            if (gameState.getCurrentRound() >= gameState.getMaxRounds()) {
                endGame(gameId, winnerToken);
            } else {
                // Prepare next round
                prepareNextRound(gameId, winnerToken);
            }
        }
    }
    
    /**
     * Register a guess from a player by token and check if the round/game is complete
     * @param gameId ID of the game
     * @param playerToken Token of the player making the guess
     * @param latitude Latitude of the guess
     * @param longitude Longitude of the guess
     */
    public void registerGuessByToken(Long gameId, String playerToken, double latitude, double longitude) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        // Submit the guess
        submitGuess(gameId, playerToken, latitude, longitude);
        
        // Broadcast guess to all players
        String username = authService.getUserByToken(playerToken).getProfile().getUsername();

        
        // Calculate distance
        LatLngDTO targetCoords = gameState.getCurrentLatLngDTO();
        LatLngDTO guessCoords = new LatLngDTO(latitude, longitude);
        int distanceMeters = (int) Math.round(calculateDistance(targetCoords, guessCoords));
        
        // Broadcast the guess
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + gameId + "/game",
            new WebSocketMessage<>("GUESS_SUBMITTED", Map.of(
                "username", username,
                "latitude", latitude,
                "longitude", longitude,
                "distance", distanceMeters,
                "round", gameState.getCurrentRound()
            ))
        );
        
        log.info("Player with token {} ({}) registered guess in game {} with distance {}m",
                playerToken, username, gameId, distanceMeters);
        
        // If all players have submitted guesses, automatically determine winner
        if (areAllGuessesSubmitted(gameId)) {
            String winnerToken = determineRoundWinner(gameId);
            User winnerUser = authService.getUserByToken(winnerToken);
            // 2) pull the real username (fallback if something’s wrong)
            String winnerUsername = winnerUser != null
                ? winnerUser.getProfile().getUsername()
                : "UNKNOWN";
            // 3) debug‐log what we’re about to send
            log.debug("Determined round‐winner via AuthService: token={}, username={}", 
            winnerToken, winnerUsername);
            int winningDistance = gameState.getPlayerGuesses().get(winnerToken);
            
            // 4) broadcast with the real name
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + gameId + "/game",
                new RoundWinnerBroadcast(winnerUsername, gameState.getCurrentRound(), winningDistance)
            );
            log.info("Broadcasted RoundWinnerBroadcast: username={}, …", winnerUsername);
            
            
            // Game will continue to the next round or end if the winner has no round cards left
            // This logic is now entirely handled in prepareNextRound
            prepareNextRound(gameId, winnerToken);
        }
    }
    
    /**
     * Determine the winner of the current round
     * @param gameId ID of the game
     * @return Token of the winning player
     */
    public String determineRoundWinner(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        // FIXED: Use a synchronized block to avoid race conditions
        synchronized (gameState.getPlayerGuesses()) {
            // FIXED: Add a more robust check with helpful error messages
            if (gameState.getStatus() != GameStatus.WAITING_FOR_GUESSES) {
                throw new IllegalStateException(
                    String.format("Game is not in guessing phase, current status: %s", gameState.getStatus()));
            }
            
            if (gameState.getPlayerGuesses().size() < gameState.getPlayerTokens().size()) {
                // FIXED: Include detailed information about which players are missing guesses
                Set<String> missingTokens = new HashSet<>(gameState.getPlayerTokens());
                missingTokens.removeAll(gameState.getPlayerGuesses().keySet());
                
                throw new IllegalStateException(
                    String.format("Not all players have submitted their guesses. %d/%d received. Missing: %s", 
                                 gameState.getPlayerGuesses().size(), 
                                 gameState.getPlayerTokens().size(),
                                 String.join(", ", missingTokens)));
            }
            
            // Find the player with the smallest distance (lowest guess value)
            String winnerToken = gameState.getPlayerGuesses().entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new IllegalStateException("Could not determine a winner"));
                
            // Store winning distance
            int winningDistance = gameState.getPlayerGuesses().get(winnerToken);
            
            // Update game state
            gameState.setLastRoundWinnerToken(winnerToken);
            gameState.setStatus(GameStatus.ROUND_COMPLETE);
            gameState.setCurrentScreen("REVEAL");
            
            // Store the winning distance in the gameState for the client
            gameState.setLastRoundWinningDistance(winningDistance);
            
            log.info("Player with token {} won the round in game {} with distance {}m", 
                    winnerToken, gameId, winningDistance);
            
            return winnerToken;
        }
    }
    
    /**
     * Prepare for the next round.
     * 1) If the winning player also *played* the round card, discard it.
     * 2) If that was their last card, end the game.
     * 3) Otherwise reset state and move on to the ROUNDCARD phase.
     */
    public void prepareNextRound(Long gameId, String nextTurnPlayerToken) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        if (gameState.getStatus() != GameStatus.ROUND_COMPLETE) {
            throw new IllegalStateException("Current round is not complete");
        }

        // ─── 1) DISCARD LOGIC ─────────────────────────────────────────────────────
        String playedBy   = gameState.getCurrentRoundCardPlayer();
        String playedCard = gameState.getCurrentRoundCardId();
        if (playedBy != null
            && playedCard != null
            && playedBy.equals(nextTurnPlayerToken)) {
            log.info("Winner {} played card {} → discarding it", nextTurnPlayerToken, playedCard);

            // Remove from DB
            roundCardService.removeRoundCardFromPlayerByToken(gameId, nextTurnPlayerToken, playedCard);
            // Remove from in‐memory inventory
            gameState.getInventoryForPlayer(nextTurnPlayerToken)
                    .getRoundCards()
                    .remove(playedCard);
        }

        // ─── 2) END‐GAME CHECK ───────────────────────────────────────────────────
        if (!hasRoundCards(gameId, nextTurnPlayerToken)) {
            log.info("Player {} has no more round cards → ending game", nextTurnPlayerToken);
            endGame(gameId, nextTurnPlayerToken);
            return;
        }

        // ─── 3) RESET FOR NEXT ROUND ─────────────────────────────────────────────
        gameState.setCurrentTurnPlayerToken(nextTurnPlayerToken);
        gameState.setLastRoundWinnerToken(nextTurnPlayerToken);

        // Clear out any leftover pointers & coordinates
        gameState.setCurrentRoundCard(null);
        gameState.setCurrentLatLngDTO(null);
        gameState.setActiveRoundCard(null);

        // Clear guesses and tracking
        gameState.getPlayerGuesses().clear();
        resetRoundTracking(gameId);

        // Jump back to the ROUNDCARD phase
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
        gameState.setCurrentScreen("ROUNDCARD");

        // Broadcast updated state
        sendGameStateToAll(gameId);
        log.info("Prepared for next round in game {}, next chooser: {}", gameId, nextTurnPlayerToken);
    }
    
    /**
     * End the game with a winner
     * @param gameId    ID of the game
     * @param winnerToken Token of the winning player
     */
    public void endGame(Long gameId, String winnerToken) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }

        gameState.setStatus(GameStatus.GAME_OVER);
        gameState.setGameWinnerToken(winnerToken);
        gameState.setCurrentScreen("GAMEOVER");  // Set screen to game over

        // Lookup the winner's username via AuthService instead of PlayerInfo
        String winnerUsername = authService
            .getUserByToken(winnerToken)
            .getProfile()
            .getUsername();

        log.info("Game {} ended, winner username: {}, token: {}", gameId, winnerUsername, winnerToken);

        // Send structured game winner broadcast
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + gameId + "/game",
            new GameWinnerBroadcast(winnerUsername)
        );

        // Notify all clients of the final game state
        sendGameStateToAll(gameId);

        // Schedule cleanup of game resources after some time
        CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(() -> {
            cleanupGame(gameId);
        });
    }

    
    /**
     * Clean up game resources when a game is complete
     * @param gameId ID of the game to clean up
     */
    public void cleanupGame(Long gameId) {
        gameStates.remove(gameId);
        cardsPlayedInRound.remove(gameId);
        punishmentsInRound.remove(gameId);
        cardPlayDetails.remove(gameId);
        log.info("Cleaned up resources for game {}", gameId);
    }
    
    /**
     * Start the game and set initial state
     * @param gameId ID of the game
     * @param maxRounds Maximum number of rounds
     */
    public void startGame(Long gameId, int maxRounds) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            throw new IllegalStateException("Game not initialized: " + gameId);
        }
        
        // Set max rounds
        gameState.setMaxRounds(maxRounds);
        gameState.setCurrentScreen("ROUNDCARD"); // Ensure we start with round card screen
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD); // Ensure proper status is set
        
        // Send initial game state to all players
        sendGameStateToAll(gameId);
        
        log.info("Game {} started with {} players, {} rounds",
                 gameId, gameState.getPlayerTokens().size(), maxRounds);
    }
    
    /**
     * Send game state to a specific user
     * @param gameId ID of the game
     * @param playerToken Token of the user to send state to
     */
    public void sendGameStateToUser(Long gameId, String playerToken) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.warn("Could not send game state - game {} not found", gameId);
            return;
        }
        
        try {
            // Create state for client
            Map<String, Object> responseState = new HashMap<>();
            responseState.put("currentRound", gameState.getCurrentRound());
            responseState.put("currentScreen", gameState.getCurrentScreen());
            
            // FIXED: Always include roundCardSubmitter if in ROUNDCARD phase and current turn player exists
            if (gameState.getRoundCardSubmitter() != null) {
                responseState.put("roundCardSubmitter", gameState.getRoundCardSubmitter());
            } else if ("ROUNDCARD".equals(gameState.getCurrentScreen())
                    && gameState.getCurrentTurnPlayerToken() != null) {
                try {
                    User currentTurnPlayer = authService.getUserByToken(
                        gameState.getCurrentTurnPlayerToken()
                    );
                    String username = currentTurnPlayer.getProfile().getUsername();
                    responseState.put("roundCardSubmitter", username);
                    
                    // Persist it for future consistency
                    gameState.setRoundCardSubmitter(username);
                    log.info("Derived roundCardSubmitter in sendGameStateToUser: {}", username);
                } catch (Exception e) {
                    log.error("Failed to derive roundCardSubmitter: {}", e.getMessage());
                    responseState.put("roundCardSubmitter", null);
                }
            } else {
                responseState.put("roundCardSubmitter", null);
            }
            
            responseState.put("activeRoundCard", gameState.getActiveRoundCard());
            responseState.put("currentTurnPlayerToken", gameState.getCurrentTurnPlayerToken());
            
            // Inventory for this player
            GameState.PlayerInventory inventory = gameState.getInventoryForPlayer(playerToken);
            if (inventory != null) {
                Map<String, List<String>> inventoryMap = new HashMap<>();
                inventoryMap.put(
                  "roundCards",
                  inventory.getRoundCards() != null ? inventory.getRoundCards() : List.of()
                );
                inventoryMap.put(
                  "actionCards",
                  inventory.getActionCards() != null ? inventory.getActionCards() : List.of()
                );
                responseState.put("inventory", inventoryMap);
            } else {
                responseState.put("inventory", Map.of(
                    "roundCards", List.of(),
                    "actionCards", List.of()
                ));
            }
            
            // Guess‐screen attrs
            Map<String, Object> guessScreenAttrs = new HashMap<>();
            guessScreenAttrs.put("time", gameState.getGuessScreenAttributes().getTime());
            if (gameState.getGuessScreenAttributes().getLatitude() != 0) {
                Map<String, Double> guessLocation = new HashMap<>();
                guessLocation.put("lat", gameState.getGuessScreenAttributes().getLatitude());
                guessLocation.put("lon", gameState.getGuessScreenAttributes().getLongitude());
                guessScreenAttrs.put("guessLocation", guessLocation);
            }
            if (gameState.getGuessScreenAttributes().getResolveResponse() != null) {
                guessScreenAttrs.put(
                  "resolveResponse",
                  gameState.getGuessScreenAttributes().getResolveResponse()
                );
            }
            responseState.put("guessScreenAttributes", guessScreenAttrs);
            
            // Player list
            List<Map<String, Object>> playersArray = new ArrayList<>();
            for (Map.Entry<String, GameState.PlayerInfo> entry
                 : gameState.getPlayerInfo().entrySet()) {
                GameState.PlayerInfo info = entry.getValue();
                Map<String, Object> playerInfo = new HashMap<>();
                playerInfo.put("username", info.getUsername());
                playerInfo.put("roundCardsLeft", info.getRoundCardsLeft());
                playerInfo.put("actionCardsLeft", info.getActionCardsLeft());
                playerInfo.put("activeActionCards", info.getActiveActionCards());
                playersArray.add(playerInfo);
            }
            responseState.put("players", playersArray);
            
            // **Log and send**
            log.info("📨 Sending GAME_STATE to user {} (round={}, screen={})",
                     playerToken,
                     gameState.getCurrentRound(),
                     gameState.getCurrentScreen());
            messagingTemplate.convertAndSendToUser(
                playerToken,
                "/queue/lobby/" + gameId + "/game/state",
                new WebSocketMessage<>("GAME_STATE", responseState)
            );
        } catch (Exception e) {
            log.error("Error sending game state to user {}: {}", playerToken, e.getMessage(), e);
        }
    }
    
    
    /**
     * Send game state to a specific user by token
     * @param gameId ID of the game
     * @param userToken Token of the user to send state to
     */
    public void sendGameStateToUserByToken(Long gameId, String userToken) {
        sendGameStateToUser(gameId, userToken);
    }
    
    /**
     * Send game state to all players in the game
     * @param gameId ID of the game
     */
    public void sendGameStateToAll(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) return;
        
        for (String playerToken : gameState.getPlayerTokens()) {
            sendGameStateToUser(gameId, playerToken);
        }
    }
    
    /**
     * Calculate the distance between two LatLngDTO in meters
     */
    private double calculateDistance(LatLngDTO coord1, LatLngDTO coord2) {
        // Implementation of the Haversine formula
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(coord2.getLatitude() - coord1.getLatitude());
        double lonDistance = Math.toRadians(coord2.getLongitude() - coord1.getLongitude());
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(coord1.getLatitude())) 
                * Math.cos(Math.toRadians(coord2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
                
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c * 1000; // Convert to meters
    }
    
    /**
     * Checks if a player has already played a card in the current round
     * @param gameId ID of the game
     * @param playerToken Token of the user
     * @return true if the player has already played a card in this round
     */
    public boolean isCardPlayedInCurrentRound(Long gameId, String playerToken) {
        if (!cardsPlayedInRound.containsKey(gameId)) {
            return false;
        }
        return cardsPlayedInRound.get(gameId).contains(playerToken);
    }
    
    /**
     * Checks if a player has been punished this round with a specific card
     * @param gameId ID of the game
     * @param targetPlayerToken Token of the targeted player
     * @param cardId ID of the card used for punishment
     * @return true if the player has been punished with this card
     */
    public boolean isPlayerPunishedThisRound(Long gameId, String targetPlayerToken, String cardId) {
        if (!punishmentsInRound.containsKey(gameId)) {
            return false;
        }
        
        Map<String, Set<String>> gamePunishments = punishmentsInRound.get(gameId);
        if (!gamePunishments.containsKey(targetPlayerToken)) {
            return false;
        }
        
        return gamePunishments.get(targetPlayerToken).contains(cardId);
    }
    
    /**
     * Marks that a player has played a card this round
     * @param gameId ID of the game
     * @param playerToken Token of the user who played the card
     * @param cardId ID of the played card
     * @param targetPlayerToken Token of the target player (if applicable)
     */
    public void markCardPlayedThisRound(Long gameId, String playerToken, String cardId, String targetPlayerToken) {
        // Record that user played a card this round
        cardsPlayedInRound.computeIfAbsent(gameId, k -> new HashSet<>()).add(playerToken);
        
        // Record card play details
        Map<String, Map<String, String>> gameCardPlays = cardPlayDetails.computeIfAbsent(gameId, k -> new HashMap<>());
        Map<String, String> userCardPlays = gameCardPlays.computeIfAbsent(playerToken, k -> new HashMap<>());
        userCardPlays.put(cardId, targetPlayerToken);
        
        // If targeting another player for punishment, record it
        if (targetPlayerToken != null) {
            Map<String, Set<String>> gamePunishments = punishmentsInRound.computeIfAbsent(gameId, k -> new HashMap<>());
            gamePunishments.computeIfAbsent(targetPlayerToken, k -> new HashSet<>()).add(cardId);
        }
        
        log.info("Player with token {} played card {} targeting player {} in game {}", playerToken, cardId, targetPlayerToken, gameId);
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
     * Get the count of played cards in the current round
     * @param gameId ID of the game
     * @return the count of played cards
     */
    public int getPlayedCardCount(Long gameId) {
        return cardsPlayedInRound
                 .getOrDefault(gameId, Collections.emptySet())
                 .size();
    }
    
    /**
     * Check if all players have submitted their guesses for the current round
     * @param gameId ID of the game
     * @return true if all players have submitted guesses, false otherwise
     */
    public boolean areAllGuessesSubmitted(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) {
            log.error("Game not found for lobby {} when checking guesses", gameId);
            return false;
        }
        
        synchronized (gameState.getPlayerGuesses()) {
            int guessCount = gameState.getPlayerGuesses().size();
            int playerCount = gameState.getPlayerTokens().size();
            
            // FIXED: Log all submitted player tokens for debugging
            String submittedTokens = String.join(", ", gameState.getPlayerGuesses().keySet());
            String allTokens = String.join(", ", gameState.getPlayerTokens());
            
            log.info("Checking if all guesses submitted for game {}: {}/{} guesses received. Submitted by: [{}], Expected: [{}]",
                    gameId, guessCount, playerCount, submittedTokens, allTokens);
            
            // FIXED: Additional validation to ensure every player token has a guess
            boolean allPlayersSubmitted = true;
            for (String token : gameState.getPlayerTokens()) {
                if (!gameState.getPlayerGuesses().containsKey(token)) {
                    log.warn("Player with token {} has not yet submitted a guess", token);
                    allPlayersSubmitted = false;
                }
            }
            
            return allPlayersSubmitted && guessCount >= playerCount;
        }
    }
    
    /**
     * Check if a player has already submitted a guess in the current round
     * @param gameId ID of the game
     * @param playerToken Token of the player to check
     * @return true if the player has submitted a guess
     */
    public boolean hasPlayerSubmittedGuess(Long gameId, String playerToken) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) return false;
        
        return gameState.getPlayerGuesses().containsKey(playerToken);
    }

    /**
     * Get all player tokens for a game
     * @param gameId ID of the game
     * @return List of player tokens
     */
    public List<String> getPlayerTokens(Long gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState == null) return Collections.emptyList();
        
        return gameState.getPlayerTokens();
    }
    
    /**
     * Class to track the state of a game
     */
    public static class GameState {
        // Original properties
        private List<String> playerTokens;
        private String currentTurnPlayerToken;
        private RoundCardDTO currentRoundCard;
        private LatLngDTO currentLatLngDTO; // Changed from Coordinates
        private final Map<String, Integer> playerGuesses = new HashMap<>();
        private String lastRoundWinnerToken;
        private String gameWinnerToken;
        private GameStatus status;
        
        // New properties as per requirements
        private int currentRound = 0;
        private String currentScreen = "ROUNDCARD"; // ROUNDCARD, ACTIONCARD, GUESS, REVEAL
        private String roundCardSubmitter; // Username of player who submitted the round card
        private String activeRoundCard; // RoundCardId
        private int lastRoundWinningDistance; // Winning distance of the last round
        
        // Player-specific inventory
        private Map<String, PlayerInventory> inventories = new HashMap<>();
        
        // Guess screen attributes
        private GuessScreenAttributes guessScreenAttributes = new GuessScreenAttributes();
        
        // Enhanced player information
        private Map<String, PlayerInfo> playerInfo = new HashMap<>();
        
        // New property for max rounds
        private int maxRounds = 5; // Default max rounds
        
        // New properties for round card tracking
        private String currentRoundCardPlayer; // Store who played the round card
        private String currentRoundCardId;     // Store which card was played
        
        // Inner classes for structured data
        public static class PlayerInventory {
            private List<String> roundCards = new ArrayList<>(); // RoundCardId[]
            private List<String> actionCards = new ArrayList<>(); // ActionCardId[]
            
            public List<String> getRoundCards() {
                return roundCards;
            }
            
            public void setRoundCards(List<String> roundCards) {
                this.roundCards = roundCards;
            }
            
            public List<String> getActionCards() {
                return actionCards;
            }
            
            public void setActionCards(List<String> actionCards) {
                this.actionCards = actionCards;
            }
        }
        
        public static class GuessScreenAttributes {
            private int time; // Time left for guessing in seconds
            private ResolveResponse resolveResponse;
            private double latitude;  // Target latitude for this round 
            private double longitude; // Target longitude for this round
            
            public int getTime() {
                return time;
            }
            
            public void setTime(int time) {
                this.time = time;
            }
            
            public ResolveResponse getResolveResponse() {
                return resolveResponse;
            }
            
            public void setResolveResponse(ResolveResponse resolveResponse) {
                this.resolveResponse = resolveResponse;
            }
            
            public double getLatitude() {
                return latitude;
            }
            
            public void setLatitude(double latitude) {
                this.latitude = latitude;
            }
            
            public double getLongitude() {
                return longitude;
            }
            
            public void setLongitude(double longitude) {
                this.longitude = longitude;
            }
        }
        
        public static class ResolveResponse {
            private String id; // ActionCardId
            private String message;
            
            public String getId() {
                return id;
            }
            
            public void setId(String id) {
                this.id = id;
            }
            
            public String getMessage() {
                return message;
            }
            
            public void setMessage(String message) {
                this.message = message;
            }
        }
        
        public static class PlayerInfo {
            private String username;
            private int roundCardsLeft;
            private int actionCardsLeft;
            private List<String> activeActionCards = new ArrayList<>(); // ActionCardId[]
            
            public String getUsername() {
                return username;
            }
            
            public void setUsername(String username) {
                this.username = username;
            }
            
            public int getRoundCardsLeft() {
                return roundCardsLeft;
            }
            
            public void setRoundCardsLeft(int roundCardsLeft) {
                this.roundCardsLeft = roundCardsLeft;
            }
            
            public int getActionCardsLeft() {
                return actionCardsLeft;
            }
            
            public void setActionCardsLeft(int actionCardsLeft) {
                this.actionCardsLeft = actionCardsLeft;
            }
            
            public List<String> getActiveActionCards() {
                return activeActionCards;
            }
            
            public void setActiveActionCards(List<String> activeActionCards) {
                this.activeActionCards = activeActionCards;
            }
        }
        
        // Getters and setters
        public List<String> getPlayerTokens() {
            return playerTokens;
        }
        
        public void setPlayerTokens(List<String> playerTokens) {
            this.playerTokens = playerTokens;
        }
        
        public String getCurrentTurnPlayerToken() {
            return currentTurnPlayerToken;
        }
        
        public void setCurrentTurnPlayerToken(String currentTurnPlayerToken) {
            this.currentTurnPlayerToken = currentTurnPlayerToken;
        }
        
        public RoundCardDTO getCurrentRoundCard() {
            return currentRoundCard;
        }
        
        public void setCurrentRoundCard(RoundCardDTO currentRoundCard) {
            this.currentRoundCard = currentRoundCard;
        }
        
        public LatLngDTO getCurrentLatLngDTO() {
            return currentLatLngDTO;
        }
        
        public void setCurrentLatLngDTO(LatLngDTO currentLatLngDTO) {
            this.currentLatLngDTO = currentLatLngDTO;
        }
        
        public Map<String, Integer> getPlayerGuesses() {
            return playerGuesses;
        }
        
        public String getLastRoundWinnerToken() {
            return lastRoundWinnerToken;
        }
        
        public void setLastRoundWinnerToken(String lastRoundWinnerToken) {
            this.lastRoundWinnerToken = lastRoundWinnerToken;
        }
        
        public String getGameWinnerToken() {
            return gameWinnerToken;
        }
        
        public void setGameWinnerToken(String gameWinnerToken) {
            this.gameWinnerToken = gameWinnerToken;
        }
        
        public GameStatus getStatus() {
            return status;
        }
        
        public void setStatus(GameStatus status) {
            this.status = status;
        }
        
        public int getCurrentRound() {
            return currentRound;
        }
        
        public void setCurrentRound(int currentRound) {
            this.currentRound = currentRound;
        }
        
        public String getCurrentScreen() {
            return currentScreen;
        }
        
        public void setCurrentScreen(String currentScreen) {
            this.currentScreen = currentScreen;
        }
        
        public String getRoundCardSubmitter() {
            return roundCardSubmitter;
        }
        
        public void setRoundCardSubmitter(String roundCardSubmitter) {
            this.roundCardSubmitter = roundCardSubmitter;
        }
        
        public String getActiveRoundCard() {
            return activeRoundCard;
        }
        
        public void setActiveRoundCard(String activeRoundCard) {
            this.activeRoundCard = activeRoundCard;
        }
        
        public int getLastRoundWinningDistance() {
            return lastRoundWinningDistance;
        }
        
        public void setLastRoundWinningDistance(int lastRoundWinningDistance) {
            this.lastRoundWinningDistance = lastRoundWinningDistance;
        }
        
        public PlayerInventory getInventoryForPlayer(String playerToken) {
            return inventories.computeIfAbsent(playerToken, k -> new PlayerInventory());
        }
        
        public Map<String, PlayerInventory> getInventories() {
            return inventories;
        }
        
        public GuessScreenAttributes getGuessScreenAttributes() {
            return guessScreenAttributes;
        }
        
        public void setGuessScreenAttributes(GuessScreenAttributes guessScreenAttributes) {
            this.guessScreenAttributes = guessScreenAttributes;
        }
        
        public PlayerInfo getPlayerInfo(String playerToken) {
            return playerInfo.computeIfAbsent(playerToken, k -> new PlayerInfo());
        }
        
        public Map<String, PlayerInfo> getPlayerInfo() {
            return playerInfo;
        }
        
        public int getMaxRounds() {
            return maxRounds;
        }
        
        public void setMaxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
        }
        
        public String getCurrentRoundCardPlayer() {
            return currentRoundCardPlayer;
        }
        
        public void setCurrentRoundCardPlayer(String currentRoundCardPlayer) {
            this.currentRoundCardPlayer = currentRoundCardPlayer;
        }
        
        public String getCurrentRoundCardId() {
            return currentRoundCardId;
        }
        
        public void setCurrentRoundCardId(String currentRoundCardId) {
            this.currentRoundCardId = currentRoundCardId;
        }
    }
    
    /**
     * Enum for game status
     */
    public enum GameStatus {
        WAITING_FOR_ROUND_CARD,
        WAITING_FOR_ACTION_CARDS,
        WAITING_FOR_GUESSES,
        ROUND_COMPLETE,
        GAME_OVER
    }
}
