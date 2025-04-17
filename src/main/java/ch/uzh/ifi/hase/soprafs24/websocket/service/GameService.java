package ch.uzh.ifi.hase.soprafs24.websocket.service;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.game.GameInstance;

/**
 * Orchestrates game instances per lobby, preloading players to avoid lazy-loading issues.
 */
@Service
public class GameService {
    private final Map<Long, GameInstance> games = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final UserService userService;

    @Autowired
    public GameService(SimpMessagingTemplate messagingTemplate,
                       LobbyService lobbyService,
                       UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    /**
     * Starts a new game for the given lobby, capturing player IDs up front.
     */
    @Transactional(readOnly = true)
    public void startGame(Long lobbyId, int roundCount, int roundTime) {
        // Preload all current player IDs inside a transaction
        List<Long> playerIds = lobbyService.getLobbyById(lobbyId)
                                           .getPlayers()
                                           .stream()
                                           .map(User::getId)
                                           .collect(Collectors.toList());

        GameInstance game = new GameInstance(
            lobbyId,
            roundCount,
            roundTime,
            playerIds,
            messagingTemplate,
            lobbyService,
            userService
        );
        games.put(lobbyId, game);
        game.start();
    }

    /**
     * Delegates a user's guess to the running game instance.
     */
    public void handleGuess(Long lobbyId, Long userId, Integer guess) {
        GameInstance game = games.get(lobbyId);
        if (game != null) {
            game.registerGuess(userId, guess);
        }
    }
}
