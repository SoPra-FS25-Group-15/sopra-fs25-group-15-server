package ch.uzh.ifi.hase.soprafs24.websocket.game;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Handles per-lobby game logic: scheduling rounds, tracking scores,
 * broadcasting events, and announcing round & game winners.
 */
public class GameInstance {
    private final Long lobbyId;
    private final int roundCount;
    private final int roundTime;      // seconds
    private final List<Long> playerIds;

    private int currentRound = 0;
    private int currentTarget;
    private final Set<Long> guessedThisRound = ConcurrentHashMap.newKeySet();

    private final Map<Long, Integer> totalScores = new ConcurrentHashMap<>();
    private final Map<Long, List<GuessResultDTO>> guessHistories = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final UserService userService;

    public GameInstance(Long lobbyId,
                        int roundCount,
                        int roundTime,
                        List<Long> playerIds,
                        SimpMessagingTemplate messagingTemplate,
                        LobbyService lobbyService,
                        UserService userService) {
        this.lobbyId = lobbyId;
        this.roundCount = roundCount;
        this.roundTime = roundTime;
        this.playerIds = new ArrayList<>(playerIds);
        this.messagingTemplate = messagingTemplate;
        this.lobbyService = lobbyService;
        this.userService = userService;
    }

    /**
     * Kick off the game: initial 10s countdown before round 1.
     */
    public void start() {
        scheduler.schedule(this::startNextRound, 10, TimeUnit.SECONDS);
    }

    private void startNextRound() {
        currentRound++;
        currentTarget = 1 + new Random().nextInt(100);
        guessedThisRound.clear();

        // Notify start of round
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + lobbyId + "/game",
            new RoundStartMessage(currentRound)
        );

        // Schedule end-of-round
        scheduler.schedule(this::endRound, roundTime, TimeUnit.SECONDS);
    }

    private void endRound() {
        // Record timeouts for players who didn't guess
        for (Long uid : playerIds) {
            if (!guessedThisRound.contains(uid)) {
                recordGuess(uid, null);
                broadcastGuess(
                    userService.getPublicProfile(uid).getProfile().getUsername(),
                    null
                );
            }
        }

        // Compile per-round results
        List<PlayerResultDTO> results = playerIds.stream()
            .map(uid -> {
                String name = userService.getPublicProfile(uid).getProfile().getUsername();
                int score = totalScores.getOrDefault(uid, 0);
                List<GuessResultDTO> history = guessHistories.getOrDefault(uid, Collections.emptyList());
                return new PlayerResultDTO(name, score, history);
            })
            .collect(Collectors.toList());

        // Broadcast round results
        boolean gameEnd = (currentRound >= roundCount);
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + lobbyId + "/game",
            new RoundResultsMessage(gameEnd, results)
        );

        // Determine and announce round winner (ignore null guesses)
        playerIds.stream()
          .filter(uid -> guessHistories.getOrDefault(uid, Collections.emptyList())
                            .get(currentRound-1).getGuess() != null)
          .min(Comparator.comparingInt(uid -> guessHistories.get(uid)
                            .get(currentRound-1).getDiff()))
          .ifPresent(winnerId -> {
            String winnerName = userService.getPublicProfile(winnerId)
                                  .getProfile().getUsername();
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/game",
                new RoundWinnerBroadcast(winnerName, currentRound)
            );
          });

        // If game ends, announce overall winner
        if (gameEnd) {
            playerIds.stream()
              .min(Comparator.comparingInt(uid -> totalScores.getOrDefault(uid, Integer.MAX_VALUE)))
              .ifPresent(winnerId -> {
                String winnerName = userService.getPublicProfile(winnerId)
                                      .getProfile().getUsername();
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/game",
                    new GameWinnerBroadcast(winnerName)
                );
              });
        }

        // Continue to next round if not finished
        if (!gameEnd) {
            startNextRound();
        }
    }

    /**
     * Record and broadcast a guess.
     */
    public void registerGuess(Long userId, Integer guess) {
        if (guessedThisRound.add(userId)) {
            recordGuess(userId, guess);
            String name = userService.getPublicProfile(userId).getProfile().getUsername();
            broadcastGuess(name, guess);
        }
    }

    private void recordGuess(Long userId, Integer guess) {
        int diff = (guess == null ? Integer.MAX_VALUE : Math.abs(guess - currentTarget));
        totalScores.merge(userId, diff, Integer::sum);
        guessHistories
          .computeIfAbsent(userId, k -> new ArrayList<>())
          .add(new GuessResultDTO(guess, (diff == Integer.MAX_VALUE ? null : diff)));
    }

    private void broadcastGuess(String username, Integer guess) {
        Integer diff = (guess == null ? null : Math.abs(guess - currentTarget));
        messagingTemplate.convertAndSend(
            "/topic/lobby/" + lobbyId + "/game",
            new RoundGuessBroadcast(username, guess, diff, currentRound)
        );
    }
}
