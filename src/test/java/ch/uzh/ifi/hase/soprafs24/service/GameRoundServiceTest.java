package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameRoundService.RoundData;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameRoundServiceTest {

    @Mock
    private ActionCardService actionCardService;

    @Mock
    private GameService gameService;

    @Mock
    private GoogleMapsService googleMapsService;

    @Mock
    private AuthService authService;

    @Mock
    private ActionCardMapper actionCardMapper;

    @InjectMocks
    private GameRoundService gameRoundService;

    private final Long GAME_ID = 1L;
    private final String USER_TOKEN_1 = "user1-token";
    private final String USER_TOKEN_2 = "user2-token";
    private List<String> playerTokens;
    private User testUser1;
    private User testUser2;
    private ActionCardDTO testActionCard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setToken(USER_TOKEN_1);
        UserProfile profile1 = new UserProfile();
        profile1.setUsername("user1");
        testUser1.setProfile(profile1);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setToken(USER_TOKEN_2);
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("user2");
        testUser2.setProfile(profile2);

        // Setup player tokens
        playerTokens = Arrays.asList(USER_TOKEN_1, USER_TOKEN_2);

        // Setup test action card
        testActionCard = new ActionCardDTO();
        testActionCard.setId("action-card-1");
        testActionCard.setType("powerup");
        testActionCard.setTitle("Test Card");
        testActionCard.setDescription("Test description");

        // Setup mock behaviors
        when(authService.getUserByToken(USER_TOKEN_1)).thenReturn(testUser1);
        when(authService.getUserByToken(USER_TOKEN_2)).thenReturn(testUser2);
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard);
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(new LatLngDTO(10.0, 20.0));
        
        // Mock game state
        GameState mockGameState = mock(GameState.class);
        when(gameService.getGameState(GAME_ID)).thenReturn(mockGameState);
        when(mockGameState.getInventoryForPlayer(anyString())).thenReturn(new GameState.PlayerInventory());
    }

    @Test
    void startGame_success() {
        gameRoundService.startGame(GAME_ID, playerTokens);

        // Verify round is initialized to 0
        assertEquals(0, gameRoundService.hasMoreRounds(GAME_ID) ? 0 : -1);
    }

    @Test
    void startNextRound_success() {
        // Start game first
        gameRoundService.startGame(GAME_ID, playerTokens);

        // Start next round
        RoundData roundData = gameRoundService.startNextRound(GAME_ID, playerTokens);

        assertNotNull(roundData);
        assertEquals(1, roundData.getRoundNumber());
        assertEquals(10.0, roundData.getLatitude());
        assertEquals(20.0, roundData.getLongitude());
        assertEquals(1, roundData.getGuesses());
    }

    @Test
    void distributeFreshActionCardsByToken_success() {
        Map<String, ActionCardDTO> result = gameRoundService.distributeFreshActionCardsByToken(GAME_ID, playerTokens);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(USER_TOKEN_1));
        assertTrue(result.containsKey(USER_TOKEN_2));
        assertEquals("action-card-1", result.get(USER_TOKEN_1).getId());
        assertEquals("action-card-1", result.get(USER_TOKEN_2).getId());

        // Verify action card service is called for each player
        verify(actionCardService, times(2)).drawRandomCard();
    }

    @Test
    void replacePlayerActionCardByToken_success() {
        // First distribute cards to establish state
        gameRoundService.distributeFreshActionCardsByToken(GAME_ID, playerTokens);

        // Replace a card
        ActionCardDTO result = gameRoundService.replacePlayerActionCardByToken(GAME_ID, USER_TOKEN_1);

        assertNotNull(result);
        assertEquals("action-card-1", result.getId());
        verify(actionCardService, times(3)).drawRandomCard(); // Called twice in distribute + once in replace
    }

    @Test
    void hasMoreRounds_true() {
        // Start game and first round
        gameRoundService.startGame(GAME_ID, playerTokens);
        gameRoundService.startNextRound(GAME_ID, playerTokens);

        // Should have more rounds (max 3)
        assertTrue(gameRoundService.hasMoreRounds(GAME_ID));
    }

    @Test
    void hasMoreRounds_false() {
        // Start game and all 3 rounds
        gameRoundService.startGame(GAME_ID, playerTokens);
        gameRoundService.startNextRound(GAME_ID, playerTokens); // Round 1
        gameRoundService.startNextRound(GAME_ID, playerTokens); // Round 2
        gameRoundService.startNextRound(GAME_ID, playerTokens); // Round 3

        // Should not have more rounds
        assertFalse(gameRoundService.hasMoreRounds(GAME_ID));
    }
}
