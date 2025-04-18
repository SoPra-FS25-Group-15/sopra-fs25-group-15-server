package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@Controller
public class ActionCardWebSocketController {

    @Autowired
    private ActionCardService actionCardService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Client sends to /app/game/{gameId}/actionCards/random
     * Server validates token header and pushes one card on /topic/game/{gameId}/actionCards/random
     */
    @MessageMapping("/game/{gameId}/actionCards/random")
    public void drawRandomCard(
            @DestinationVariable Long gameId,
            StompHeaderAccessor headerAccessor,
            Principal principal) {

        // Extract raw "Authorization" header from the STOMP CONNECT or SEND
        String auth = headerAccessor.getFirstNativeHeader("Authorization");
        userService.getUserByToken(auth.replaceFirst("^Bearer ", "")); // validate

        ActionCardDTO card = actionCardService.drawRandomCard();
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/actionCards/random",
            card
        );
    }
}
