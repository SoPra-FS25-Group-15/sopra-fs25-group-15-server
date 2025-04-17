package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.ActionCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * WebSocket controller for Action Card related operations
 */
@Controller
public class WebSocketActionCardHandler {

    private final Logger log = LoggerFactory.getLogger(WebSocketActionCardHandler.class);

    private final ActionCardService actionCardService;
    private final UserService userService;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final ActionCardRepository actionCardRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketActionCardHandler(ActionCardService actionCardService,
                                      UserService userService,
                                      GameRepository gameRepository,
                                      UserRepository userRepository,
                                      ActionCardRepository actionCardRepository,
                                      SimpMessagingTemplate messagingTemplate) {
        this.actionCardService = actionCardService;
        this.userService = userService;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.actionCardRepository = actionCardRepository;
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
            String actionCardId = payload.get("actionCardId");
            String toUserName = payload.get("toUserName"); // For punishment cards

            // Validate type
            if (!"ACTION_CARD_SUBMIT".equals(type)) {
                sendErrorToUser(token, "Invalid message type");
                return;
            }

            // Skip option - when player chooses not to play any card
            if ("SKIP".equals(actionCardId)) {
                handleSkipActionCard(gameId, token);
                return;
            }

            // Get user from token
            User user = userService.getUserByToken(token);
            if (user == null) {
                sendErrorToUser(token, "Invalid token");
                return;
            }

            // Get game
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

            // Validate that user is in the game
            if (!game.getPlayers().contains(user)) {
                sendErrorToUser(token, "You are not a player in this game");
                return;
            }

            // Find the action card
            ActionCard actionCard = null;
            try {
                Long cardId = Long.parseLong(actionCardId);
                actionCard = actionCardRepository.findById(cardId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action card not found"));
            } catch (NumberFormatException e) {
                sendErrorToUser(token, "Invalid action card ID");
                return;
            }

            // Check if the user owns the card
            if (!actionCard.getOwner().getId().equals(user.getId())) {
                sendErrorToUser(token, "You don't have this card in your hand");
                return;
            }

            // For punishment cards, validate target player
            User targetPlayer = null;
            if (actionCard.getType() == ActionCardType.PUNISHMENT) {
                if (toUserName == null || toUserName.isEmpty()) {
                    sendErrorToUser(token, "Please select a player this card should be applied to");
                    return;
                }

                targetPlayer = userService.findByUsername(toUserName);
                if (targetPlayer == null) {
                    sendErrorToUser(token, "Target player not found");
                    return;
                }

                if (!game.getPlayers().contains(targetPlayer)) {
                    sendErrorToUser(token, "Target player is not in this game");
                    return;
                }

                if (targetPlayer.getId().equals(user.getId())) {
                    sendErrorToUser(token, "You can't apply a punishment card to yourself");
                    return;
                }
            }

            // Create DTO for service call
            ActionCardPlayDTO playDTO = new ActionCardPlayDTO();
            playDTO.setActionCardId(actionCard.getId());
            if (targetPlayer != null) {
                playDTO.setTargetPlayerId(targetPlayer.getId());
            }

            // Apply card effect
            ActionCardEffectDTO effectDTO = actionCardService.playActionCard(gameId, user.getId(), playDTO);

            // Send success message to the player
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/games/" + gameId + "/action-card-result",
                    Map.of(
                            "status", "success",
                            "message", "Card played successfully",
                            "effect", effectDTO
                    )
            );

            // Notify all players about the card effect (this is already done by actionCardService)

        } catch (Exception e) {
            log.error("Error handling action card submit", e);
            sendErrorToUser(payload.get("token"), "An error occurred: " + e.getMessage());
        }
    }

    /**
     * Handle when a player chooses to skip playing any action card
     */
    private void handleSkipActionCard(Long gameId, String token) {
        try {
            User user = userService.getUserByToken(token);
            if (user == null) {
                sendErrorToUser(token, "Invalid token");
                return;
            }

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

    /**
     * Helper method to send error message to specific user
     */
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
}