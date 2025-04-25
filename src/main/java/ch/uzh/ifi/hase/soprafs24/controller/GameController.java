package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.GameDataResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/games")
public class GameController {

    private final RoundCardService roundCardService;
    private final ActionCardService actionCardService;
    private final LobbyService lobbyService;
    private final AuthService authService;

    @Autowired
    public GameController(RoundCardService roundCardService,
                          ActionCardService actionCardService,
                          LobbyService lobbyService,
                          AuthService authService) {
        this.roundCardService = roundCardService;
        this.actionCardService = actionCardService;
        this.lobbyService = lobbyService;
        this.authService = authService;
    }

    /**
     * GET /games/data
     * Returns the initial game data including round cards and action cards
     */
    @GetMapping("/data")
    @ResponseStatus(HttpStatus.OK)
    public GameDataResponseDTO getGameData(@RequestParam Long lobbyId,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            // 1) Clean out any "Bearer " prefix
            String token = TokenUtils.extractToken(authHeader);

            // 2) Validate token and load user
            var user = authService.getUserByToken(token);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
            }

            // 3) Check membership by token (host or in players list)
            boolean isHost = lobbyService.isUserHostByToken(lobbyId, token);
            var playerTokens = lobbyService.getLobbyPlayerTokens(lobbyId);
            if (!isHost && !playerTokens.contains(token)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this lobby");
            }

            // 4) Build response
            GameDataResponseDTO response = new GameDataResponseDTO();

            // Static round cards
            List<RoundCardDTO> roundCards = roundCardService.getAllRoundCards();
            response.setRoundCards(roundCards);

            // One fresh action card per player
            Map<Long, List<ActionCardDTO>> actionMap = new HashMap<>();
            var playerIds = lobbyService.getLobbyPlayerIds(lobbyId);
            for (Long pid : playerIds) {
                var cards = new ArrayList<ActionCardDTO>();
                cards.add(actionCardService.drawRandomCard());
                actionMap.put(pid, cards);
            }
            response.setActionCards(actionMap);

            return response;
        } catch (ResponseStatusException ex) {
            // Re-throw ResponseStatusException as is - these are already properly formatted errors
            throw ex;
        } catch (RuntimeException ex) {
            // Convert all other runtime exceptions to INTERNAL_SERVER_ERROR
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An error occurred processing your request: " + ex.getMessage()
            );
        }
    }
}
