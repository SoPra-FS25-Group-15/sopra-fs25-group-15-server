package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState.PlayerInfo;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameStatus;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserXpService;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService;
import ch.uzh.ifi.hase.soprafs24.service.GameRoundService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class GameServiceStatsTest {

    private static final Long GAME_ID = 123L;
    private static final String WINNER_TOKEN = "winner";
    private static final String LOSER_TOKEN  = "loser";

    @Mock private GoogleMapsService googleMapsService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GameRoundService gameRoundService;
    @Mock private AuthService authService;
    @Mock private UserXpService userXpService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void determineRoundWinner_incrementsGamesPlayedAndWins() {
        // Arrange: set up GameState with two players and guesses
        GameState state = new GameState();
        state.setPlayerTokens(Arrays.asList(WINNER_TOKEN, LOSER_TOKEN));
        state.setStatus(GameStatus.WAITING_FOR_GUESSES);
        state.getPlayerGuesses().put(WINNER_TOKEN, 10);
        state.getPlayerGuesses().put(LOSER_TOKEN, 20);
        // ensure PlayerInfo entries exist
        state.getPlayerInfo().put(WINNER_TOKEN, new PlayerInfo());
        state.getPlayerInfo().put(LOSER_TOKEN, new PlayerInfo());

        // inject into gameService
        Map<Long, GameState> map = new ConcurrentHashMap<>();
        map.put(GAME_ID, state);
        ReflectionTestUtils.setField(gameService, "gameStates", map);

        // prepare User stats
        User winner = new User();
        winner.setEmail("winner@example.com");
        winner.setPassword("test");
        UserProfile wp = new UserProfile();
        wp.setGamesPlayed(0);
        wp.setWins(0);
        winner.setProfile(wp);

        User loser = new User();
        loser.setEmail("loser@example.com");
        loser.setPassword("test");
        UserProfile lp = new UserProfile();
        lp.setGamesPlayed(5);
        lp.setWins(1);
        loser.setProfile(lp);

        when(authService.getUserByToken(WINNER_TOKEN)).thenReturn(winner);
        when(authService.getUserByToken(LOSER_TOKEN)).thenReturn(loser);

        // Act
        String result = gameService.determineRoundWinner(GAME_ID);

        // Assert: correct winner returned
        assertEquals(WINNER_TOKEN, result);

        // Loser: gamesPlayed +1, wins unchanged
        assertEquals(6, loser.getProfile().getGamesPlayed());
        assertEquals(1, loser.getProfile().getWins());
        verify(userRepository).save(loser);

        // Winner: gamesPlayed +1, wins +1
        assertEquals(1, winner.getProfile().getGamesPlayed());
        assertEquals(1, winner.getProfile().getWins());
        verify(userRepository).save(winner);
    }
}
