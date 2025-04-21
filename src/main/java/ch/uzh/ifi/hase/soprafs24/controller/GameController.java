package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.GameDataResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/games")
public class GameController {

    private final ActionCardService actionCardService;
    private final RoundCardService roundCardService;
    private final LobbyService lobbyService;
    private final UserService userService;

    @Autowired
    public GameController(
            ActionCardService actionCardService,
            RoundCardService roundCardService,
            LobbyService lobbyService,
            UserService userService) {
        this.actionCardService = actionCardService;
        this.roundCardService = roundCardService;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    /**
     * GET /games/data
     * Returns the initial game data including round cards and action cards
     */
    @GetMapping("/data")
    @ResponseStatus(HttpStatus.OK)
    public GameDataResponseDTO getGameData(@RequestParam Long lobbyId, 
                                          @RequestHeader("Authorization") String token) {
        // Validate user access to lobby
        User currentUser = userService.getUserByToken(token);
        
        // Check user is in the lobby
        if (!lobbyService.isUserInLobby(currentUser.getId(), lobbyId)) {
            throw new RuntimeException("You are not a member of this lobby");
        }
        
        // Get all players in the lobby
        List<Long> playerIds = lobbyService.getLobbyPlayerIds(lobbyId);
        
        GameDataResponseDTO response = new GameDataResponseDTO();
        
        // Add round cards
        response.setRoundCards(roundCardService.getAllRoundCards());
        
        // Generate one action card per player
        Map<Long, List<ActionCardDTO>> playerActionCards = new HashMap<>();
        for (Long playerId : playerIds) {
            List<ActionCardDTO> cards = new ArrayList<>();
            cards.add(actionCardService.drawRandomCard());
            playerActionCards.put(playerId, cards);
        }
        response.setActionCards(playerActionCards);
        
        return response;
    }
}
