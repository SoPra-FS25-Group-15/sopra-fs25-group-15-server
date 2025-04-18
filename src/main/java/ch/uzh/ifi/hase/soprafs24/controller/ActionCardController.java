package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game/{gameId}/actionCards")
public class ActionCardController {

    private final ActionCardService actionCardService;
    private final UserService userService;

    public ActionCardController(ActionCardService actionCardService,
                                UserService userService) {
        this.actionCardService = actionCardService;
        this.userService = userService;
    }

    /**
     * GET /game/{gameId}/actionCards/random
     * Validate the Authorization token and return one of the two hard‑coded cards
     * at random for the authenticated player.
     */
    @GetMapping("/random")
    @ResponseStatus(HttpStatus.OK)
    public ActionCardDTO drawRandom(@PathVariable Long gameId,
                                    @RequestHeader("Authorization") String token) {
        // validate player
        userService.getUserByToken(token);
        return actionCardService.drawRandomCard();
    }
}
