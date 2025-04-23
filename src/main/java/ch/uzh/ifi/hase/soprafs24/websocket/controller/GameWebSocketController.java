package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.GameRoundService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RoundStartDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.StartGameRequestDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RoundWinnerBroadcast;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GameWinnerBroadcast;

@Controller
public class GameWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketController.class);
    
    @Autowired
    private GameService gameService;
    
    @Autowired
    private GameRoundService gameRoundService;
    
    @Autowired
    private RoundCardService roundCardService;
    
    @Autowired
    private ActionCardService actionCardService;
    
    @Autowired
    private LobbyService lobbyService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;

    /**
     * Verify user token is valid
     */
    private String validateAuthentication(Principal principal) {
        if (principal == null) {
            log.error("Unauthorized WebSocket request - missing principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        String principalName = principal.getName();
        log.debug("Validating authentication for principal: {}", TokenUtils.maskToken(principalName));
        
        // If the principal is a number (user ID), try to find the token
        if (principalName != null && principalName.matches("\\d+")) {
            log.debug("Principal appears to be a user ID: {}. Checking for token.", principalName);
            try {
                User user = userService.getPublicProfile(Long.parseLong(principalName));
                if (user != null && user.getToken() != null) {
                    log.debug("Found token for user ID: {}", principalName);
                    return user.getToken();
                }
            } catch (Exception e) {
                log.error("Error retrieving token for user ID {}: {}", principalName, e.getMessage());
            }
        }
        
        // Otherwise, extract token (handling Bearer prefix if present)
        try {
            String token = TokenUtils.extractToken(principalName);
            User user = authService.getUserByToken(token);
            if (user == null) {
                log.error("Invalid token: {}", TokenUtils.maskToken(token));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
            }
            return token;
        } catch (ResponseStatusException e) {
            log.error("Token validation failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Get user token from StompHeaderAccessor
     */
    private String getUserTokenFromHeaders(StompHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
            return (String) sessionAttributes.get("token");
        }
        List<String> authHeaders = headerAccessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }
        return null;
    }

    /**
     * Allow host to start game as soon as ≥2 players have joined.
     */
    @MessageMapping("/lobby/{lobbyId}/game/start")
    @Transactional
    public void startGame(@DestinationVariable Long lobbyId,
                          @Payload WebSocketMessage<StartGameRequestDTO> message,
                          Principal principal) {
        String userToken = validateAuthentication(principal);
        User user = authService.getUserByToken(userToken);
        
        if (!lobbyService.isUserHostByToken(lobbyId, userToken)) {
            log.warn("Only the host can start the game: lobby={}, userToken={}", lobbyId, userToken);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can start the game");
        }

        var lobby = lobbyService.getLobbyById(lobbyId);
        var players = lobbyService.getLobbyPlayerIds(lobbyId);
        int playerCount = players.size();
        log.info("Starting game request for lobby {}: {} players present", lobbyId, playerCount);

        // **Changed**: require only 2 players instead of full lobby
        if (playerCount < 2) {
            log.warn("Cannot start game - need at least 2 players but found {}", playerCount);
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("Need at least 2 players to start (only %d joined)", playerCount)
            );
        }

        // Distribute round cards
        Map<String, List<RoundCardDTO>> tokenToRoundCards = new HashMap<>();
        List<String> playerTokens = players.stream()
            .map(id -> userService.getPublicProfile(id).getToken())
            .collect(Collectors.toList());
        for (String token : playerTokens) {
            var cards = roundCardService.assignRoundCardsToPlayer(token);
            tokenToRoundCards.put(token, cards);
        }

        // Select random starter
        Random random = new Random();
        String startingToken = playerTokens.get(random.nextInt(playerTokens.size()));
        log.info("Randomly selected starting player token: {}", startingToken);

        // Initialize game & distribute action cards
        gameService.initializeGame(lobbyId, playerTokens, startingToken);
        var actionCards = gameRoundService.distributeFreshActionCardsByToken(lobbyId, playerTokens);

        // Populate initial inventories & state
        for (String token : playerTokens) {
            var rc = tokenToRoundCards.get(token);
            List<String> rcIds = rc.stream().map(RoundCardDTO::getId).collect(Collectors.toList());
            gameService.getGameState(lobbyId).getInventoryForPlayer(token).setRoundCards(rcIds);

            var ac = actionCards.get(token);
            List<String> acIds = ac != null ? List.of(ac.getId()) : List.of();
            gameService.getGameState(lobbyId).getInventoryForPlayer(token).setActionCards(acIds);
        }

        // Broadcast GAME_START so all clients reroute
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + lobbyId + "/game",
            new WebSocketMessage<>("GAME_START", Map.of("startingPlayerToken", startingToken))
        );
        log.info("Broadcasted GAME_START for lobby {} with starting token {}", lobbyId, startingToken);

        // Mark lobby in-progress
        lobbyService.updateLobbyStatus(lobbyId, "IN_PROGRESS");
    }

    /**
     * Handle round card selection
     */
    @MessageMapping("/lobby/{lobbyId}/game/select-round-card")
    public void selectRoundCard(@DestinationVariable Long lobbyId,
                                @Payload Map<String, Object> payload,
                                Principal principal) {
        final String userToken = validateAuthentication(principal);
        final String originalRoundCardId = (String) payload.get("roundCardId");
        
        log.info("Round card selection request received: lobbyId={}, roundCardId={}", lobbyId, originalRoundCardId);
        
        try {
            // Verify this is a valid user
            User user = authService.getUserByToken(userToken);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
            }

            // Get current game state
            GameService.GameState gameState = gameService.getGameState(lobbyId);
            if (gameState == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
            }
            
            // Verify it's this player's turn
            if (!userToken.equals(gameState.getCurrentTurnPlayerToken())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
            }
            
            // Verify we're in the right phase
            if (!gameState.getCurrentScreen().equals("ROUNDCARD")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Not in round card selection phase, current screen: " + gameState.getCurrentScreen());
            }
            
            // Find the card in player's inventory - with type-based fallback
            RoundCardDTO selectedCard = null;
            final String cardType;
            String roundCardId = originalRoundCardId;
            
            // Extract the card type from the ID (world, flash, etc.)
            if (originalRoundCardId != null && originalRoundCardId.contains("-")) {
                cardType = originalRoundCardId.substring(0, originalRoundCardId.indexOf('-'));
                log.info("Extracted card type '{}' from ID '{}'", cardType, originalRoundCardId);
            } else {
                cardType = null;
            }
            
            // Get the player's cards
            List<RoundCardDTO> playerCards = roundCardService.getPlayerRoundCardsByToken(lobbyId, userToken);
            log.info("Player with token {} has {} cards in game {}", userToken, playerCards.size(), lobbyId);
            
            // Print all card IDs for debugging
            if (!playerCards.isEmpty()) {
                log.info("Available card IDs for player {}: {}", userToken, 
                        playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.toList()));
            }
            
            // First try exact match
            selectedCard = playerCards.stream()
                    .filter(card -> card.getId().equals(originalRoundCardId))
                    .findFirst()
                    .orElse(null);
                    
            // If exact match failed but we have the card type, try to find by type
            if (selectedCard == null && cardType != null) {
                // Create a final copy for use in lambda
                final String searchCardType = cardType;
                
                selectedCard = playerCards.stream()
                    .filter(card -> card.getId().startsWith(searchCardType + "-"))
                    .findFirst()
                    .orElse(null);
                    
                if (selectedCard != null) {
                    log.info("Found card by type: {}", selectedCard.getId());
                    roundCardId = selectedCard.getId(); // Update roundCardId to the found card's ID
                }
            }
                    
            if (selectedCard == null) {
                log.error("Round card {} not found in player's inventory after type-based search", originalRoundCardId);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "Round card not found in inventory")
                );
                return;
            }
            
            // Create final copy for use later
            final String finalRoundCardId = roundCardId;
            final RoundCardDTO finalSelectedCard = selectedCard;
            
            // Record who played the card and which card was used - this helps with tracking
            gameState.setRoundCardSubmitter(user.getProfile().getUsername());
            gameState.setCurrentRoundCardPlayer(userToken);
            gameState.setCurrentRoundCardId(finalRoundCardId);
            
            // FIXED: Before starting round, ensure no active round card and null coordinates
            gameState.setActiveRoundCard(null);
            gameState.setCurrentLatLngDTO(null);
            
            // Start the round with this card - this will fetch coordinates
            try {
                LatLngDTO coordinates = gameService.startRound(lobbyId, finalSelectedCard);
                log.info("Round started with coordinates: {}, {}", coordinates.getLatitude(), coordinates.getLongitude());
                
                // Extract time limit from card modifiers
                int timeLimit = 30; // Default
                if (finalSelectedCard.getModifiers() != null && finalSelectedCard.getModifiers().getTime() > 0) {
                    timeLimit = finalSelectedCard.getModifiers().getTime();
                }
                
                // Set active round card in game state
                gameState.setActiveRoundCard(finalRoundCardId);
                
                // FIXED: Remove the card from player's inventory - it's been played
                roundCardService.removePlayerRoundCard(lobbyId, userToken, finalRoundCardId);
                
                // Broadcast round card selection to all players
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>(
                        "ROUND_CARD_SELECTED", 
                        Map.of(
                            "roundCard", finalSelectedCard,
                            "playerToken", userToken,
                            "username", user.getProfile().getUsername()
                        )
                    )
                );
                
                // After a short delay, move to action card phase
                // This ensures clients have time to process the round card selection
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // First send a screen transition message
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("SCREEN_CHANGE", Map.of(
                        "screen", "ACTIONCARD",
                        "roundCardComplete", true
                    ))
                );
                
                // Then broadcast action card phase start with timeLimit
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>(
                        "ACTION_CARD_PHASE_START", 
                        Map.of(
                            "timeLimit", timeLimit,
                            "coordinates", Map.of(
                                "latitude", coordinates.getLatitude(),
                                "longitude", coordinates.getLongitude()
                            )
                        )
                    )
                );
                
                // Send updated game state to all players
                gameService.sendGameStateToAll(lobbyId);
                
                log.info("Successfully processed round card selection: lobbyId={}, roundCardId={}, coordinates={},{}",
                        lobbyId, finalRoundCardId, coordinates.getLatitude(), coordinates.getLongitude());
            } catch (Exception e) {
                log.error("Error starting round with selected card: {}", e.getMessage(), e);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "Error starting round: " + e.getMessage())
                );
            }
        } catch (ResponseStatusException ex) {
            log.error("Error processing round card selection: {}", ex.getMessage());
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ERROR", ex.getReason())
            );
        } catch (Exception ex) {
            log.error("Unexpected error processing round card selection: {}", ex.getMessage(), ex);
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ERROR", "Unexpected error: " + ex.getMessage())
            );
        }
    }

    /**
     * Handle transition from action card phase to guessing phase once all players have submitted
     */
    @MessageMapping("/lobby/{lobbyId}/game/action-cards-complete")
    public void actionCardsComplete(@DestinationVariable Long lobbyId,
                              Principal principal) {
        String userToken = validateAuthentication(principal);
        
        log.info("Action cards complete notification received for lobby: {}", lobbyId);
        
        try {
            // Start the guessing phase
            gameService.startGuessingPhase(lobbyId);
            var state = gameService.getGameState(lobbyId);
            
            // Create round start DTO with coordinates and time
            var roundStart = new RoundStartDTO();
            roundStart.setRound(state.getCurrentRound());
            roundStart.setLatitude(state.getGuessScreenAttributes().getLatitude());
            roundStart.setLongitude(state.getGuessScreenAttributes().getLongitude());
            roundStart.setRoundTime(state.getGuessScreenAttributes().getTime());
            roundStart.setStartTimer(true);  // Explicitly indicate the timer should start
            
            // Get active action cards for each player to include in transition message
            Map<String, List<String>> playerActiveCards = new HashMap<>();
            state.getPlayerInfo().forEach((token, info) -> {
                playerActiveCards.put(token, info.getActiveActionCards());
            });

            // 1. Send SCREEN_CHANGE message first - explicit instruction to change screens
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("SCREEN_CHANGE", Map.of(
                    "screen", "GUESS",
                    "actionCardsComplete", true
                ))
            );
            
            // 2. Then send ROUND_START with all necessary information
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ROUND_START", Map.of(
                    "roundData", roundStart,
                    "activeActionCards", playerActiveCards,
                    "startGuessTimer", true
                ))
            );
            
            // Update game state for all players
            gameService.sendGameStateToAll(lobbyId);
            
            log.info("Transitioned to guessing phase for game {}, round time: {}s", 
                     lobbyId, state.getGuessScreenAttributes().getTime());
        } catch (Exception e) {
            log.error("Error transitioning to guessing phase: {}", e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ERROR", "Error starting guessing phase: " + e.getMessage())
            );
        }
    }
    
    /**
     * Handle round time expiration to determine the winner
     */
    @MessageMapping("/lobby/{lobbyId}/game/round-time-expired")
    public void roundTimeExpired(@DestinationVariable Long lobbyId,
                               Principal principal) {
        String userToken = validateAuthentication(principal);
        
        try {
            log.info("Round time expired for lobby {}, handling end of round", lobbyId);
            
            GameService.GameState gameState = gameService.getGameState(lobbyId);
            if (gameState == null) {
                log.error("Game state not found for lobby {}", lobbyId);
                return;
            }
            
            // Verify we're in the guessing phase
            if (!gameState.getCurrentScreen().equals("GUESS") || 
                gameState.getStatus() != GameService.GameStatus.WAITING_FOR_GUESSES) {
                log.warn("Round time expired but not in guessing phase. Current screen: {}, status: {}", 
                        gameState.getCurrentScreen(), gameState.getStatus());
                return;
            }
            
            // FIXED: Add synchronization for thread safety when checking/modifying game state
            synchronized(gameState) {
                // Get all player tokens
                List<String> allPlayerTokens = gameService.getPlayerTokens(lobbyId);
                
                // Log current guess state before proceeding
                log.info("Current guess state before processing expirations: submitted guesses={}, total players={}", 
                         gameState.getPlayerGuesses().size(), allPlayerTokens.size());
                
                // FIXED: Only generate default guesses if they haven't already been submitted
                boolean anyDefaultGuessesGenerated = false;
                
                // Check if any players haven't submitted guesses and generate default guesses for them
                for (String playerToken : allPlayerTokens) {
                    if (!gameService.hasPlayerSubmittedGuess(lobbyId, playerToken)) {
                        anyDefaultGuessesGenerated = true;
                        log.info("Player with token {} has not submitted a guess, generating default guess", playerToken);
                        
                        // Get the target coordinates from the game state
                        double targetLat = gameState.getGuessScreenAttributes().getLatitude();
                        double targetLon = gameState.getGuessScreenAttributes().getLongitude();
                        
                        // Generate a default guess that's far from the target (to ensure they don't win)
                        double defaultLat = (targetLat + 40) % 90;
                        double defaultLon = (targetLon + 60) % 180;
                        
                        // Register this default guess
                        try {
                            gameService.registerGuessByToken(lobbyId, playerToken, defaultLat, defaultLon);
                            log.info("Generated default guess for player {}: lat={}, lon={}", 
                                     playerToken, defaultLat, defaultLon);
                        }
                        catch (Exception e) {
                            log.error("Failed to register default guess for player {}: {}", 
                                     playerToken, e.getMessage());
                        }
                    }
                }
                
                // FIXED: Add a short delay if default guesses were generated to ensure they're processed
                if (anyDefaultGuessesGenerated) {
                    try {
                        log.info("Added short delay after generating default guesses to ensure state consistency");
                        Thread.sleep(100); 
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Now determine the round winner - all players should have guesses now
                if (gameService.areAllGuessesSubmitted(lobbyId)) {
                    log.info("All players have guesses (including defaults), determining round winner");
                    try {
                        String winnerToken = gameService.determineRoundWinner(lobbyId);
                        if (winnerToken != null) {
                            // Get the winner's username and winning distance
                            String winnerUsername = gameState.getPlayerInfo().get(winnerToken).getUsername();
                            int winningDistance = gameState.getPlayerGuesses().get(winnerToken);
                            
                            // Broadcast round winner with structured message
                            messagingTemplate.convertAndSend(
                                "/topic/lobby/" + lobbyId + "/game",
                                new RoundWinnerBroadcast(winnerUsername, gameState.getCurrentRound(), winningDistance)
                            );
                            
                            log.info("Broadcast round winner: {}, distance: {}m", winnerUsername, winningDistance);
                            
                            // Process round completion - handles round card removal logic
                            processRoundCompletion(lobbyId, winnerToken);
                            
                            // Update game state to ensure it reflects the winner
                            gameService.sendGameStateToAll(lobbyId);
                        } else {
                            log.error("determineRoundWinner returned null despite all guesses being submitted");
                        }
                    } catch (Exception e) {
                        log.error("Error determining round winner: {}", e.getMessage(), e);
                        messagingTemplate.convertAndSendToUser(
                            userToken, 
                            "/queue/lobby/" + lobbyId + "/game",
                            new WebSocketMessage<>("ERROR", "Error determining round winner: " + e.getMessage())
                        );
                    }
                } else {
                    log.error("Failed to determine round winner - not all players have guesses after forced submission.");
                    log.error("Current status: {}/{} guesses registered", 
                              gameState.getPlayerGuesses().size(), allPlayerTokens.size());
                }
            }
        } catch (Exception e) {
            log.error("Error processing round time expiration: {}", e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ERROR", "Error processing round time: " + e.getMessage())
            );
        }
    }

    @Transactional
    @MessageMapping("/lobby/{lobbyId}/game/play-action-card")
    public void playActionCard(@DestinationVariable Long lobbyId,
                               @Payload Map<String, Object> payload,
                               Principal principal) {
        String userToken = validateAuthentication(principal);
        String actionCardId = (String) payload.get("actionCardId");
        String targetPlayerToken = (String) payload.get("targetPlayerToken");

        log.info("Action card play request received: lobbyId={}, actionCardId={}, targetPlayerToken={}", 
                 lobbyId, actionCardId, targetPlayerToken);

        try {
            if (gameService.isCardPlayedInCurrentRound(lobbyId, userToken)) {
                log.warn("Player with token {} already played a card in this round", userToken);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "You already played a card this round")
                );
                return;
            }
            
            if (targetPlayerToken != null && gameService.isPlayerPunishedThisRound(lobbyId, targetPlayerToken, actionCardId)) {
                log.warn("Target player with token {} already has punishment {}", targetPlayerToken, actionCardId);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "Target player already has this punishment")
                );
                return;
            }
            
            // Validate if this is a valid action card
            if (!actionCardService.isValidActionCard(actionCardId)) {
                log.error("Invalid action card ID: {}", actionCardId);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "Invalid action card ID")
                );
                return;
            }
            
            // Mark the card as played in the current round
            gameService.markCardPlayedThisRound(lobbyId, userToken, actionCardId, targetPlayerToken);
            
            // Process the action card effect - handle transactions properly
            ActionCardEffectDTO effectDTO = actionCardService.processActionCardForGame(
                lobbyId, userToken, actionCardId, targetPlayerToken);
            
            if (effectDTO == null) {
                log.error("Failed to process action card effect for card: {}", actionCardId);
                messagingTemplate.convertAndSendToUser(
                    userToken,
                    "/queue/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ERROR", "Failed to process action card effect")
                );
                return;
            }
            
            // Create effect payload based on card type
            Map<String, Object> effect = new HashMap<>();
            effect.put("cardId", actionCardId);
            effect.put("playerToken", userToken);
            
            if (targetPlayerToken != null) {
                effect.put("targetPlayerToken", targetPlayerToken);
                
                if ("badsight".equals(actionCardId)) {
                    effect.put("effect", "blur");
                    effect.put("duration", 15);
                }
            } else {
                if ("7choices".equals(actionCardId)) {
                    var gameState = gameService.getGameState(lobbyId);
                    double lat = gameState.getGuessScreenAttributes().getLatitude();
                    double lon = gameState.getGuessScreenAttributes().getLongitude();
                    String continent = actionCardService.getContinent(lat, lon);
                    effect.put("effect", "continent");
                    effect.put("value", continent);
                }
            }

            // Broadcast that this player played an action card
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ACTION_CARD_PLAYED", effect)
            );
            
            // Replace player's action card
            ActionCardDTO newCard = gameRoundService.replacePlayerActionCardByToken(lobbyId, userToken);
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game/action-card",
                new WebSocketMessage<>("ACTION_CARD_REPLACEMENT", newCard)
            );

            // Acknowledge the submission
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ACTION_CARD_SUBMIT", Map.of(
                    "playerToken", userToken,
                    "actionCardId", actionCardId
                ))
            );
            
            // Send updated game state to all players
            gameService.sendGameStateToAll(lobbyId);

            // Check if all players have played their cards
            List<String> allPlayerTokens = gameService.getPlayerTokens(lobbyId);
            int playedCount = gameService.getPlayedCardCount(lobbyId);
            
            log.info("Action card played count: {}/{}", playedCount, allPlayerTokens.size());
            
            if (playedCount >= allPlayerTokens.size()) {
                log.info("All players ({}/{}) have played action cards, starting guessing phase", 
                        playedCount, allPlayerTokens.size());
                        
                // Start the guessing phase
                gameService.startGuessingPhase(lobbyId);
                var state = gameService.getGameState(lobbyId);
                
                // Create round start DTO with coordinates and time
                var roundStart = new RoundStartDTO();
                roundStart.setRound(state.getCurrentRound());
                roundStart.setLatitude(state.getGuessScreenAttributes().getLatitude());
                roundStart.setLongitude(state.getGuessScreenAttributes().getLongitude());
                roundStart.setRoundTime(state.getGuessScreenAttributes().getTime());
                
                // Get active action cards for each player to include in transition message
                Map<String, List<String>> playerActiveCards = new HashMap<>();
                state.getPlayerInfo().forEach((token, info) -> {
                    playerActiveCards.put(token, info.getActiveActionCards());
                });

                // 1. Send SCREEN_CHANGE message first - explicit instruction to change screens
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("SCREEN_CHANGE", Map.of(
                        "screen", "GUESS",
                        "actionCardsComplete", true
                    ))
                );
                
                // 2. Then send ROUND_START with all necessary information
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new WebSocketMessage<>("ROUND_START", Map.of(
                        "roundData", roundStart,
                        "activeActionCards", playerActiveCards,
                        "startGuessTimer", true
                    ))
                );
                
                // Update game state for all players
                gameService.sendGameStateToAll(lobbyId);
                
                log.info("Sent guessing phase transition messages with round time: {}s", 
                         state.getGuessScreenAttributes().getTime());
            }
            
            log.info("Action card {} played by player with token {} in game {}", actionCardId, userToken, lobbyId);
            
        } catch (Exception e) {
            log.error("Error processing action card play: {}", e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                userToken,
                "/queue/lobby/" + lobbyId + "/game",
                new WebSocketMessage<>("ERROR", "Error playing action card: " + e.getMessage())
            );
        }
    }
    
    /**
     * Processes round completion, removing round card from winner if they played it
     * This should be called when determining the round winner
     */
    private void processRoundCompletion(Long lobbyId, String winnerToken) {
        GameService.GameState gameState = gameService.getGameState(lobbyId);
        if (gameState == null) {
            log.error("Game not found for lobby {} during round completion", lobbyId);
            return;
        }
        
        // Check if the winner is the same player who played the round card
        String roundCardPlayerToken = gameState.getCurrentRoundCardPlayer();
        String roundCardId = gameState.getCurrentRoundCardId();
        
        if (roundCardPlayerToken != null && roundCardId != null && roundCardPlayerToken.equals(winnerToken)) {
            // Winner is the same player who played the round card, remove it from their inventory
            log.info("Round winner {} is the same player who chose the round card {}. Removing card from inventory.", 
                     winnerToken, roundCardId);
                     
            boolean removed = roundCardService.removeRoundCardFromPlayerByToken(lobbyId, winnerToken, roundCardId);
            if (removed) {
                log.info("Round card {} removed from player {} because they won the round", 
                         roundCardId, winnerToken);
                
                // Check if the player has no round cards left after removal
                if (!gameService.hasRoundCards(lobbyId, winnerToken)) {
                    log.info("Player {} has no more round cards left - ending game", winnerToken);
                    String winnerUsername = gameState.getPlayerInfo().get(winnerToken).getUsername();
                    
                    // Send game winner broadcast
                    messagingTemplate.convertAndSend(
                        "/topic/lobby/" + lobbyId + "/game",
                        new GameWinnerBroadcast(winnerUsername)
                    );
                    
                    // End the game
                    gameService.endGame(lobbyId, winnerToken);
                } else {
                    // Player still has round cards, prepare for next round with this player as the active player
                    log.info("Player {} still has round cards left - preparing for next round with them as active player", 
                             winnerToken);
                    gameService.prepareNextRound(lobbyId, winnerToken);
                }
            } else {
                log.error("Failed to remove round card {} from winner {}", roundCardId, winnerToken);
                // Still prepare for next round even if the removal failed
                gameService.prepareNextRound(lobbyId, winnerToken);
            }
        } else if (roundCardPlayerToken != null && roundCardId != null) {
            // The player who played the round card didn't win, so keep the card in their inventory
            log.info("Round card {} stays with player {} because they didn't win the round",
                     roundCardId, roundCardPlayerToken);
                     
            // Winner is not the player who played the round card
            // Just prepare next round with the winner as the active player
            log.info("Preparing for next round with winner {} as the active player", winnerToken);
            gameService.prepareNextRound(lobbyId, winnerToken);
        } else {
            // Missing information about who played the round card
            log.warn("Missing round card player information during round completion");
            // Still prepare for next round with the winner as the active player
            gameService.prepareNextRound(lobbyId, winnerToken);
        }
        
        // Reset tracking for the next round
        gameState.setCurrentRoundCardPlayer(null);
        gameState.setCurrentRoundCardId(null);
    }
}
