package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Action Card Controller
 * This class is responsible for handling all REST request that are related to
 * action cards.
 * The controller will receive the request and delegate the execution to the
 * ActionCardService and finally return the result.
 */
@RestController
public class ActionCardController {

    private final ActionCardService actionCardService;
    private final UserService userService;

    ActionCardController(ActionCardService actionCardService, UserService userService) {
        this.actionCardService = actionCardService;
        this.userService = userService;
    }

    @GetMapping("/api/games/{gameId}/actionCards")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ActionCardGetDTO getActionCards(@PathVariable Long gameId, @RequestHeader("Authorization") String token) {
        // Get the user from the token
        User user = userService.getUserByToken(token);

        // Get the action cards
        List<ActionCard> actionCards = actionCardService.getPlayerActionCards(gameId, user.getId());

        // Convert to DTOs
        List<ActionCardDTO> actionCardDTOs = new ArrayList<>();
        for (ActionCard actionCard : actionCards) {
            actionCardDTOs.add(ActionCardMapper.INSTANCE.convertEntityToActionCardDTO(actionCard));
        }

        // Create and return the response
        ActionCardGetDTO response = new ActionCardGetDTO();
        response.setActionCards(actionCardDTOs);
        return response;
    }

    @PostMapping("/api/games/{gameId}/actionCards/draw")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ActionCardDTO drawActionCard(@PathVariable Long gameId, @RequestHeader("Authorization") String token) {
        // Get the user from the token
        User user = userService.getUserByToken(token);

        // Draw a new card
        ActionCard newCard = actionCardService.drawActionCard(gameId, user.getId());

        // Convert to DTO and return
        return ActionCardMapper.INSTANCE.convertEntityToActionCardDTO(newCard);
    }

    @PostMapping("/api/games/{gameId}/actionCards/play")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ActionCardEffectDTO playActionCard(
            @PathVariable Long gameId,
            @RequestBody ActionCardPlayDTO actionCardPlayDTO,
            @RequestHeader("Authorization") String token) {
        // Get the user from the token
        User user = userService.getUserByToken(token);

        // Play the card and return the effect
        return actionCardService.playActionCard(gameId, user.getId(), actionCardPlayDTO);
    }

    @GetMapping("/api/games/{gameId}/actionCards/active")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ActionCardGetDTO getActiveActionCards(@PathVariable Long gameId) {
        // Get active action cards
        List<ActionCard> activeCards = actionCardService.getActiveActionCards(gameId);

        // Convert to DTOs
        List<ActionCardDTO> actionCardDTOs = new ArrayList<>();
        for (ActionCard actionCard : activeCards) {
            actionCardDTOs.add(ActionCardMapper.INSTANCE.convertEntityToActionCardDTO(actionCard));
        }

        // Create and return the response
        ActionCardGetDTO response = new ActionCardGetDTO();
        response.setActionCards(actionCardDTOs);
        return response;
    }

    @DeleteMapping("/api/games/{gameId}/actionCards/{actionCardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardActionCard(
            @PathVariable Long gameId,
            @PathVariable Long actionCardId,
            @RequestHeader("Authorization") String token) {
        // Get the user from the token
        User user = userService.getUserByToken(token);

        // Discard the card
        actionCardService.discardActionCard(gameId, user.getId(), actionCardId);
    }
}
