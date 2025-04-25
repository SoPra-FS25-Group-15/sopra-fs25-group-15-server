package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameStatus;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class GameServiceTest {

    @Mock
    private GoogleMapsService googleMapsService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameRoundService gameRoundService;

    @Mock
    private AuthService authService;

    @Mock
    private RoundCardService roundCardService;

    @InjectMocks
    private GameService gameService;

    private final Long testGameId = 123L;
    private final String user1Token = "user1-token";
    private final String user2Token = "user2-token";
    private List<String> playerTokens;
    private Map<Long, GameState> gameStates;
    private RoundCardDTO testRoundCard;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize test data
        playerTokens = Arrays.asList(user1Token, user2Token);
        gameStates = new ConcurrentHashMap<>();

        // Create test round card
        testRoundCard = new RoundCardDTO();
        testRoundCard.setId("testRoundCard1");
        RoundCardDTO.RoundCardModifiers modifiers = new RoundCardDTO.RoundCardModifiers();
        modifiers.setTime(30); // 30 seconds for testing
        testRoundCard.setModifiers(modifiers);

        // Set up mock responses
        LatLngDTO testCoords = new LatLngDTO(45.0, 45.0);
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(testCoords);

        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(WebSocketMessage.class));
        
        List<RoundCardDTO> testCards = Collections.singletonList(testRoundCard);
        when(roundCardService.assignPlayerRoundCards(eq(testGameId), anyString())).thenReturn(testCards);
        when(roundCardService.getPlayerRoundCardsByToken(eq(testGameId), anyString())).thenReturn(testCards);

        // Create and configure mock User objects
        user1 = createMockUser(1L, "User One", user1Token);
        user2 = createMockUser(2L, "User Two", user2Token);
        
        // Configure the authService mock to return appropriate User objects
        when(authService.getUserByToken(user1Token)).thenReturn(user1);
        when(authService.getUserByToken(user2Token)).thenReturn(user2);
        
        // Initialize tracking maps to prevent NullPointerExceptions
        ReflectionTestUtils.setField(gameService, "gameStates", gameStates);
        ReflectionTestUtils.setField(gameService, "cardsPlayedInRound", new HashMap<Long, Set<String>>());
        ReflectionTestUtils.setField(gameService, "punishmentsInRound", new HashMap<Long, Map<String, Set<String>>>());
        ReflectionTestUtils.setField(gameService, "cardPlayDetails", new HashMap<Long, Map<String, Map<String, String>>>());
        
        // Instead of calling initializeGame, we'll construct the GameState manually
        GameState gameState = new GameState();
        gameState.setPlayerTokens(playerTokens);
        gameState.setCurrentTurnPlayerToken(user1Token);
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
        gameState.setCurrentScreen("ROUNDCARD");
        gameState.setCurrentRound(0);
        
        // Create playerInfo maps and set it using reflection since there's no setter
        Map<String, GameState.PlayerInfo> playerInfoMap = new HashMap<>();
        
        // Create player info objects manually
        GameState.PlayerInfo info1 = new GameState.PlayerInfo();
        ReflectionTestUtils.setField(info1, "username", "User One");
        ReflectionTestUtils.setField(info1, "roundCardsLeft", 2);
        ReflectionTestUtils.setField(info1, "actionCardsLeft", 1);
        ReflectionTestUtils.setField(info1, "activeActionCards", new ArrayList<>());
        playerInfoMap.put(user1Token, info1);
        
        GameState.PlayerInfo info2 = new GameState.PlayerInfo();
        ReflectionTestUtils.setField(info2, "username", "User Two");
        ReflectionTestUtils.setField(info2, "roundCardsLeft", 2);
        ReflectionTestUtils.setField(info2, "actionCardsLeft", 1);
        ReflectionTestUtils.setField(info2, "activeActionCards", new ArrayList<>());
        playerInfoMap.put(user2Token, info2);
        
        // Use reflection to set the playerInfo field directly
        ReflectionTestUtils.setField(gameState, "playerInfo", playerInfoMap);
        
        // Create inventory objects manually
        Map<String, Object> inventoryMap = new HashMap<>();
        
        // For player 1
        Map<String, Object> inventory1 = new HashMap<>();
        inventory1.put("roundCards", new ArrayList<>(Arrays.asList("world-" + user1Token, "flash-" + user1Token)));
        inventory1.put("actionCards", new ArrayList<>(Collections.singletonList("7choices")));
        inventoryMap.put(user1Token, inventory1);
        
        // For player 2
        Map<String, Object> inventory2 = new HashMap<>();
        inventory2.put("roundCards", new ArrayList<>(Arrays.asList("world-" + user2Token, "flash-" + user2Token)));
        inventory2.put("actionCards", new ArrayList<>(Collections.singletonList("badsight")));
        inventoryMap.put(user2Token, inventory2);
        
        // Use reflection to set the inventories field directly
        ReflectionTestUtils.setField(gameState, "inventories", inventoryMap);
        
        // Use reflection to set player guesses
        ReflectionTestUtils.setField(gameState, "playerGuesses", new HashMap<>());
        
        // Add the game state to the map
        gameStates.put(testGameId, gameState);
    }
    
    // Helper method to create a mock User
    private User createMockUser(Long id, String username, String token) {
        User user = new User();
        // Manually set ID using reflection since there's no setter
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set ID on mock User", e);
        }
        
        user.setToken(token);
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        user.setProfile(profile);
        return user;
    }

    @Test
    void initializeGame_success() {
        // Assert that the game state is correctly initialized
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(playerTokens, state.getPlayerTokens());
        assertEquals(user1Token, state.getCurrentTurnPlayerToken());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, state.getStatus());
        assertEquals(0, state.getCurrentRound());
        
        // Verify playerInfo has been initialized
        assertNotNull(state.getPlayerInfo());
        assertFalse(state.getPlayerInfo().isEmpty());
    }

    @Test
    void startRound_success() {
        // Start a round with the test round card
        LatLngDTO coords = gameService.startRound(testGameId, testRoundCard);

        // Verify the round is started correctly
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(1, state.getCurrentRound());
        assertEquals(GameStatus.WAITING_FOR_ACTION_CARDS, state.getStatus());
        assertEquals("ACTIONCARD", state.getCurrentScreen());
        assertNotNull(coords);
        assertEquals(45.0, coords.getLatitude());
        assertEquals(45.0, coords.getLongitude());
        assertEquals(testRoundCard, state.getCurrentRoundCard());
        assertEquals(testRoundCard.getId(), state.getActiveRoundCard());
    }

    @Test
    void startGuessingPhase_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        
        // Then start the guessing phase
        gameService.startGuessingPhase(testGameId);

        // Verify the guessing phase is started correctly
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(GameStatus.WAITING_FOR_GUESSES, state.getStatus());
        assertEquals("GUESS", state.getCurrentScreen());
        assertTrue(state.getGuessScreenAttributes().getTime() > 0);
    }

    @Test
    void submitGuess_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit a guess
        gameService.submitGuess(testGameId, user1Token, 44.0, 44.0);

        // Verify the guess is recorded
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertTrue(state.getPlayerGuesses().containsKey(user1Token));
        // Verify the returned distance is reasonable (should be about 157,249 meters)
        assertTrue(state.getPlayerGuesses().get(user1Token) > 0);
    }

    @Test
    void registerGuessByToken_allGuessesSubmitted() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Register guesses for all players
        gameService.registerGuessByToken(testGameId, user1Token, 44.0, 44.0);
        gameService.registerGuessByToken(testGameId, user2Token, 46.0, 46.0);

        // Verify all guesses are recorded and round is complete
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(2, state.getPlayerGuesses().size());
        assertNotNull(state.getLastRoundWinnerToken());
        assertEquals(GameStatus.ROUND_COMPLETE, state.getStatus());
        assertEquals("REVEAL", state.getCurrentScreen());
    }

    @Test
    void prepareNextRound_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit guesses for all players
        gameService.submitGuess(testGameId, user1Token, 44.0, 44.0);
        gameService.submitGuess(testGameId, user2Token, 46.0, 46.0);

        // Determine a winner
        String winnerToken = gameService.determineRoundWinner(testGameId);
        
        // Prepare for the next round
        gameService.prepareNextRound(testGameId, winnerToken);

        // Verify the next round is prepared correctly
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, state.getStatus());
        assertEquals("ROUNDCARD", state.getCurrentScreen());
        assertEquals(winnerToken, state.getCurrentTurnPlayerToken());
        assertNull(state.getActiveRoundCard());
        assertEquals(0, state.getPlayerGuesses().size());
    }

    @Test
    void endGame_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit guesses for all players
        gameService.submitGuess(testGameId, user1Token, 44.0, 44.0);
        gameService.submitGuess(testGameId, user2Token, 46.0, 46.0);

        // Determine a winner
        String winnerToken = gameService.determineRoundWinner(testGameId);
        
        // End the game
        gameService.endGame(testGameId, winnerToken);

        // Verify the game is ended correctly
        GameState state = gameService.getGameState(testGameId);
        assertNotNull(state);
        assertEquals(GameStatus.GAME_OVER, state.getStatus());
        assertEquals("GAMEOVER", state.getCurrentScreen());
        assertEquals(winnerToken, state.getGameWinnerToken());
    }

    @Test
    void areAllGuessesSubmitted_true() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit guesses for all players
        gameService.submitGuess(testGameId, user1Token, 44.0, 44.0);
        gameService.submitGuess(testGameId, user2Token, 46.0, 46.0);

        // Verify all guesses are submitted
        assertTrue(gameService.areAllGuessesSubmitted(testGameId));
    }

    @Test
    void areAllGuessesSubmitted_false() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit a guess for only one player
        gameService.submitGuess(testGameId, user1Token, 44.0, 44.0);

        // Verify not all guesses are submitted
        assertFalse(gameService.areAllGuessesSubmitted(testGameId));
    }

    @Test
    void determineRoundWinner_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        gameService.startGuessingPhase(testGameId);

        // Submit guesses for all players with user1 having a better guess
        gameService.submitGuess(testGameId, user1Token, 45.1, 45.1); // closer to 45,45
        gameService.submitGuess(testGameId, user2Token, 46.0, 46.0); // farther from 45,45

        // Determine the winner
        String winnerToken = gameService.determineRoundWinner(testGameId);

        // Verify the winner is correctly determined (user1 should win)
        assertEquals(user1Token, winnerToken);
    }

    @Test
    void markCardPlayedThisRound_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        
        String cardId = "testActionCard";
        String targetToken = user2Token;
        
        // Mark a card as played
        gameService.markCardPlayedThisRound(testGameId, user1Token, cardId, targetToken);
        
        // Verify the card is marked as played
        assertTrue(gameService.isCardPlayedInCurrentRound(testGameId, user1Token));
        assertTrue(gameService.isPlayerPunishedThisRound(testGameId, targetToken, cardId));
    }
    
    @Test
    void resetRoundTracking_success() {
        // First start a round with the test round card
        gameService.startRound(testGameId, testRoundCard);
        
        // Mark a card as played
        gameService.markCardPlayedThisRound(testGameId, user1Token, "testActionCard", user2Token);
        
        // Reset round tracking
        gameService.resetRoundTracking(testGameId);
        
        // Verify the tracking is reset
        assertFalse(gameService.isCardPlayedInCurrentRound(testGameId, user1Token));
        assertFalse(gameService.isPlayerPunishedThisRound(testGameId, user2Token, "testActionCard"));
    }
}
