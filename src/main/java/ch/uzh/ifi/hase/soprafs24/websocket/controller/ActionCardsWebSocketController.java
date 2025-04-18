package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket controller for Action Card related operations
 * Simplified version with just the core functionality
 */
@Controller
public class ActionCardsWebSocketController {

    private final Logger log = LoggerFactory.getLogger(ActionCardsWebSocketController.class);

    private final ActionCardService actionCardService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Simple in-memory tracking of played cards per game and round
    private final Map<Long, Set<Long>> gameToPlayersWhoPlayedCards = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> punishmentTargetsTracker = new ConcurrentHashMap<>();

    @Autowired
    public ActionCardsWebSocketController(ActionCardService actionCardService,
                                         UserService userService,
                                         SimpMessagingTemplate messagingTemplate) {
        this.actionCardService = actionCardService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles the submission of an action card during a game
     */
    @MessageMapping("/games/{gameId}/action-card-submit")
    public void handleActionCardSubmit(@DestinationVariable Long gameId, Map<String, String> payload) {
        log.debug("WebSocket: Action Card Submit for Game {}: {}", gameId, payload);

        try {
            // Extract data from payload
            String type = payload.get("type");
            String token = payload.get("token");
            String cardId = payload.get("cardId"); // "7choices" or "badsight"
            String targetUsername = payload.get("targetUsername"); // For punishment cards

            // Validate type
            if (!"ACTION_CARD_SUBMIT".equals(type)) {
                sendErrorToUser(token, "Invalid message type");
                return;
            }

            // Skip option - when player chooses not to play any card
            if ("SKIP".equals(cardId)) {
                handleSkipActionCard(gameId, token);
                return;
            }

            // Get user from token
            User user = userService.getUserByToken(token);
            if (user == null) {
                sendErrorToUser(token, "Invalid token");
                return;
            }
            
            // Check if user already played a card this round
            if (hasPlayerPlayedThisRound(gameId, user.getId())) {
                sendErrorToUser(token, "You can only play one action card per round");
                return;
            }

            // For punishment cards, validate target player
            User targetUser = null;
            if ("badsight".equals(cardId)) {
                if (targetUsername == null || targetUsername.isEmpty()) {
                    sendErrorToUser(token, "Please select a player this card should be applied to");
                    return;
                }

                targetUser = userService.findByUsername(targetUsername);
                if (targetUser == null) {
                    sendErrorToUser(token, "Target player not found");
                    return;
                }

                if (targetUser.getId().equals(user.getId())) {
                    sendErrorToUser(token, "You can't apply a punishment card to yourself");
                    return;
                }
                
                // Check if this punishment has already been applied to the target
                String key = gameId + "-badsight-" + targetUser.getId();
                if (isPunishmentApplied(key)) {
                    sendErrorToUser(token, "Cannot apply more than one punishment of the same type to another player");
                    return;
                }
                
                // Mark this punishment as applied
                markPunishmentApplied(key);
            }

            // Get a random card to replace the played one
            ActionCardDTO replacementCard = actionCardService.drawRandomCard();
            
            // Mark the card as played for this round
            markCardAsPlayed(gameId, user.getId());
            
            // Send effect notification to all players in the game
            Map<String, Object> effect = new HashMap<>();
            effect.put("cardId", cardId);
            effect.put("playerId", user.getId());
            effect.put("playerName", user.getProfile().getUsername());
            
            if (targetUser != null) {
                effect.put("targetId", targetUser.getId());
                effect.put("targetName", targetUser.getProfile().getUsername());
                
                // For "badsight" card
                if ("badsight".equals(cardId)) {
                    effect.put("effect", "blur");
                    effect.put("duration", 15); // 15 seconds
                }
            } else {
                // For "7choices" card
                if ("7choices".equals(cardId)) {
                    // In a real implementation, this would get the actual continent
                    String[] continents = {"Asia", "Africa", "North America", "South America", 
                                          "Antarctica", "Europe", "Australia"};
                    effect.put("effect", "continent");
                    effect.put("value", continents[new java.util.Random().nextInt(continents.length)]);
                }
            }
            
            // Broadcast the effect to all game players
            messagingTemplate.convertAndSend(
                    "/topic/games/" + gameId + "/action-cards",
                    Map.of(
                            "type", "CARD_PLAYED",
                            "cardId", cardId,
                            "playerId", user.getId().toString(),
                            "playerName", user.getProfile().getUsername(),
                            "effect", effect
                    )
            );

            // Send success message with replacement card to the player
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/games/" + gameId + "/action-card-result",
                    Map.of(
                            "status", "success",
                            "message", "Card played successfully",
                            "replacementCard", replacementCard
                    )
            );

        } catch (Exception e) {
            log.error("Error handling action card submit", e);
            sendErrorToUser(payload.get("token"), "An error occurred: " + e.getMessage());
        }
    }

    // Handle start of a new round - call this when a round starts
    public void startNewRound(Long gameId) {
        gameToPlayersWhoPlayedCards.remove(gameId);
        // Remove punishment trackers for this game
        punishmentTargetsTracker.entrySet().removeIf(entry -> entry.getKey().startsWith(gameId + "-"));
    }

    // Track played cards
    private boolean hasPlayerPlayedThisRound(Long gameId, Long userId) {
        Set<Long> playersWhoPlayed = gameToPlayersWhoPlayedCards.getOrDefault(gameId, new HashSet<>());
        return playersWhoPlayed.contains(userId);
    }
    
    private void markCardAsPlayed(Long gameId, Long userId) {
        gameToPlayersWhoPlayedCards.computeIfAbsent(gameId, k -> new HashSet<>()).add(userId);
    }
    
    // Track punishments
    private boolean isPunishmentApplied(String key) {
        return punishmentTargetsTracker.containsKey(key);
    }
    
    private void markPunishmentApplied(String key) {
        punishmentTargetsTracker.put(key, new HashSet<>());
    }

    // Helper method to send error message to specific user
    private void sendErrorToUser(String token, String errorMessage) {
        if (token == null) return;

        try {
            User user = userService.getUserByToken(token);
            if (user != null) {
                messagingTemplate.convertAndSendToUser(
                        user.getId().toString(),
                        "/queue/errors",
                        Map.of(
                                "status", "error",
                                "message", errorMessage
                        )
                );
            }
        } catch (Exception e) {
            log.error("Error sending error message", e);
        }
    }

    // Handle when a player chooses to skip playing any action card
    private void handleSkipActionCard(Long gameId, String token) {
        try {
            User user = userService.getUserByToken(token);
            if (user == null) {
                sendErrorToUser(token, "Invalid token");
                return;
            }
            
            // Mark that this player has taken their action for the round
            markCardAsPlayed(gameId, user.getId());

            // Send notification that player skipped playing a card
            messagingTemplate.convertAndSend(
                    "/topic/games/" + gameId + "/action-cards",
                    Map.of(
                            "type", "SKIP",
                            "playerId", user.getId().toString(),
                            "playerName", user.getProfile().getUsername(),
                            "message", user.getProfile().getUsername() + " chose not to play an action card"
                    )
            );

            // Send confirmation to the player
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/games/" + gameId + "/action-card-result",
                    Map.of(
                            "status", "success",
                            "message", "Skipped playing an action card"
                    )
            );

        } catch (Exception e) {
            log.error("Error handling skip action card", e);
            sendErrorToUser(token, "An error occurred: " + e.getMessage());
        }
    }
}