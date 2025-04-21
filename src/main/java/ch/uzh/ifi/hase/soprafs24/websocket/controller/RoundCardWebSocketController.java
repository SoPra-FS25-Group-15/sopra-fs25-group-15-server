package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard.RoundCardSubmitDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard.WebSocketResponseDTO;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class RoundCardWebSocketController {


    private final RoundCardService roundCardService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RoundCardWebSocketController(RoundCardService roundCardService,
                                        UserService userService,
                                        SimpMessagingTemplate messagingTemplate) {
        this.roundCardService = roundCardService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle round card submissions through WebSocket
     */
    @MessageMapping("/game/roundcard")
    @SendTo("/topic/game")
    public WebSocketResponseDTO handleRoundCardSubmission(RoundCardSubmitDTO submitDTO) {
        try {
            // Validate token and get user
            User user = userService.getUserByToken(submitDTO.getToken());

            if (user == null) {
                return createErrorResponse("Invalid token");
            }

            // Submit the round card and get updated game
            Game game = roundCardService.submitRoundCard(
                    user.getId(),
                    submitDTO.getRoundCardId(),
                    submitDTO.getGameId()
            );

            // Create response with updated game
            WebSocketResponseDTO response = new WebSocketResponseDTO();
            response.setType("ROUND_CARD_SUBMIT");
            response.setGame(game);

            return response;

        }
        catch (ResponseStatusException ex) {
            // Handle exceptions and convert to appropriate WebSocket response
            if (ex.getStatus() == HttpStatus.FORBIDDEN) {
                return createErrorResponse("Not your turn");
            }
            return createErrorResponse(ex.getReason());
        }
        catch (Exception ex) {
            return createErrorResponse("An error occurred");
        }
    }

    /**
     * Helper method to create error responses
     */
    private WebSocketResponseDTO createErrorResponse(String message) {
        WebSocketResponseDTO response = new WebSocketResponseDTO();
        response.setType("ERROR");
        response.setMessage(message);
        return response;
    }
}
