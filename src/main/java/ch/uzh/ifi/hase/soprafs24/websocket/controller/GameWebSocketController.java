package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.StartGameMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.GuessMessage;
import ch.uzh.ifi.hase.soprafs24.websocket.service.GameService;

@Controller
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    private Long validate(Principal principal) {
        if (principal == null) throw new IllegalStateException("Not authenticated");
        return Long.parseLong(principal.getName());
    }

    /** Host starts the game */
    @MessageMapping("/lobby/{lobbyId}/game/start")
    public void start(@DestinationVariable Long lobbyId,
                      @Payload StartGameMessage msg,
                      Principal principal) {
        validate(principal);
        gameService.startGame(lobbyId, msg.getRoundCount(), msg.getRoundTime());
    }

    /** Players submit a guess */
    @MessageMapping("/lobby/{lobbyId}/game/guess")
    public void guess(@DestinationVariable Long lobbyId,
                      @Payload GuessMessage msg,
                      Principal principal) {
        Long userId = validate(principal);
        gameService.handleGuess(lobbyId, userId, msg.getGuess());
    }
}
