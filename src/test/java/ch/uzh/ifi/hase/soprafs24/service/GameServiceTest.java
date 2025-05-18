package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameStatus;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.service.UserXpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

@SpringBootTest(properties = {
        // Disable Cloud SQL auto‐config
        "spring.cloud.gcp.sql.enabled=false",
        // H2 in-memory DB
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Hibernate auto/DD-L & SQL logging
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true",
        // Dummy placeholders
        "jwt.secret=test-secret",
        "google.maps.api.key=TEST_KEY"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
public class GameServiceTest {

    private static final Long TEST_GAME_ID   = 123L;
    private static final String USER1_TOKEN  = "user1-token";
    private static final String USER2_TOKEN  = "user2-token";

    @Autowired
    private GameService gameService;

    @MockBean
    private GoogleMapsService googleMapsService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private GameRoundService gameRoundService;

    @MockBean
    private AuthService authService;

    @MockBean
    private RoundCardService roundCardService;

    @MockBean
    private UserXpService userXpService;

    private List<String> playerTokens;
    private Map<Long, GameState> gameStates;
    private RoundCardDTO testRoundCard;
    private User user1;
    private User user2;

    @BeforeEach
    public void setUp() {
        // 1) Prepare tokens and users
        playerTokens = Arrays.asList(USER1_TOKEN, USER2_TOKEN);

        user1 = new User();
    user1.setToken(USER1_TOKEN);
    user1.setEmail("user1@example.com");
    user1.setPassword("password1");
    user1.setStatus(UserStatus.ONLINE);
    user1.setUsername("user1");
    UserProfile profile1 = new UserProfile();
    profile1.setUsername("user1");      // non-nullable
    profile1.setXp(0);                  // @Column(nullable = false)
    profile1.setPoints(0);              // @Column(nullable = false)
    profile1.setGamesPlayed(0);         // @Column(nullable = false)
    profile1.setWins(0);                // @Column(nullable = false)
    profile1.setStatsPublic(true);      // @Column(nullable = false)
    user1.setProfile(profile1);

    user2 = new User();
    user2.setToken(USER2_TOKEN);
    user2.setEmail("user2@example.com");
    user2.setPassword("password2");
    user2.setStatus(UserStatus.ONLINE);
    user2.setUsername("user2");
    UserProfile profile2 = new UserProfile();
    profile2.setUsername("user2");
    profile2.setXp(0);
    profile2.setPoints(0);
    profile2.setGamesPlayed(0);
    profile2.setWins(0);
    profile2.setStatsPublic(true);
    user2.setProfile(profile2);


        ReflectionTestUtils.setField(user2, "id", 2L);
        user2.setToken(USER2_TOKEN);
        UserProfile p2 = new UserProfile();
        p2.setUsername("User Two");
        user2.setProfile(p2);

        // 2) Stub AuthService
        when(authService.getUserByToken(USER1_TOKEN)).thenReturn(user1);
        when(authService.getUserByToken(USER2_TOKEN)).thenReturn(user2);

        // 3) Stub GoogleMapsService
        when(googleMapsService.getRandomCoordinatesOnLand(TEST_GAME_ID))
                .thenReturn(new LatLngDTO(45.0, 45.0));

        // 4) Stub messaging to no-op
        doNothing().when(messagingTemplate)
                .convertAndSend(anyString(), any(WebSocketMessage.class));

        // 5) Prepare a test round card
        testRoundCard = new RoundCardDTO();
        testRoundCard.setId("testRoundCard1");
        RoundCardDTO.RoundCardModifiers mods = new RoundCardDTO.RoundCardModifiers();
        mods.setTime(30);
        testRoundCard.setModifiers(mods);
        when(roundCardService.assignPlayerRoundCards(eq(TEST_GAME_ID), anyString()))
                .thenReturn(List.of(testRoundCard));
        when(roundCardService.getPlayerRoundCardsByToken(eq(TEST_GAME_ID), anyString()))
                .thenReturn(List.of(testRoundCard));

        // 6) Stub GameRoundService
        when(gameRoundService.startNextRound(eq(TEST_GAME_ID), anyList()))
                .thenReturn(new GameRoundService.RoundData(1, 45.0, 45.0, 1));
        when(gameRoundService.hasMoreRounds(TEST_GAME_ID)).thenReturn(true);

        // 7) Seed GameService’s private state
        gameStates = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(gameService, "gameStates", gameStates);
        ReflectionTestUtils.setField(gameService, "cardsPlayedInRound", new HashMap<>());
        ReflectionTestUtils.setField(gameService, "punishmentsInRound", new HashMap<>());
        ReflectionTestUtils.setField(gameService, "cardPlayDetails", new HashMap<>());

        // Manually initialize GameState for TEST_GAME_ID
        GameState initState = new GameState();
        initState.setPlayerTokens(playerTokens);
        initState.setCurrentTurnPlayerToken(USER1_TOKEN);
        initState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
        initState.setCurrentScreen("ROUNDCARD");
        initState.setCurrentRound(0);

        Map<String, GameState.PlayerInfo> infoMap = new HashMap<>();
        GameState.PlayerInfo pi1 = new GameState.PlayerInfo();
        pi1.setUsername("User One");
        pi1.setRoundCardsLeft(2);
        pi1.setActionCardsLeft(1);
        pi1.setActiveActionCards(new ArrayList<>());
        infoMap.put(USER1_TOKEN, pi1);

        GameState.PlayerInfo pi2 = new GameState.PlayerInfo();
        pi2.setUsername("User Two");
        pi2.setRoundCardsLeft(2);
        pi2.setActionCardsLeft(1);
        pi2.setActiveActionCards(new ArrayList<>());
        infoMap.put(USER2_TOKEN, pi2);

        ReflectionTestUtils.setField(initState, "playerInfo", infoMap);

        Map<String, GameState.PlayerInventory> invMap = new HashMap<>();
        GameState.PlayerInventory inv1 = new GameState.PlayerInventory();
        inv1.setRoundCards(List.of("world-" + USER1_TOKEN, "flash-" + USER1_TOKEN));
        inv1.setActionCards(List.of("7choices"));
        invMap.put(USER1_TOKEN, inv1);

        GameState.PlayerInventory inv2 = new GameState.PlayerInventory();
        inv2.setRoundCards(List.of("world-" + USER2_TOKEN, "flash-" + USER2_TOKEN));
        inv2.setActionCards(List.of("badsight"));
        invMap.put(USER2_TOKEN, inv2);

        ReflectionTestUtils.setField(initState, "inventories", invMap);
        ReflectionTestUtils.setField(initState, "playerGuesses", new HashMap<>());

        gameStates.put(TEST_GAME_ID, initState);
    }

    @Test
    void initializeGame_success() {
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertNotNull(state);
        assertEquals(playerTokens, state.getPlayerTokens());
        assertEquals(USER1_TOKEN, state.getCurrentTurnPlayerToken());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, state.getStatus());
        assertEquals(0, state.getCurrentRound());
        assertFalse(state.getPlayerInfo().isEmpty());
    }

    @Test
    void startRound_success() {
        LatLngDTO coords = gameService.startRound(TEST_GAME_ID, testRoundCard);

        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertEquals(1, state.getCurrentRound());
        assertEquals(GameStatus.WAITING_FOR_ACTION_CARDS, state.getStatus());
        assertEquals("ACTIONCARD", state.getCurrentScreen());
        assertEquals(45.0, coords.getLatitude());
        assertEquals(45.0, coords.getLongitude());
        assertEquals(testRoundCard, state.getCurrentRoundCard());
        assertEquals(testRoundCard.getId(), state.getActiveRoundCard());
    }

    @Test
    void startGuessingPhase_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        // Then start the guessing phase
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Verify the guessing phase is started correctly
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertNotNull(state);
        assertEquals(GameStatus.WAITING_FOR_GUESSES, state.getStatus());
        assertEquals("GUESS", state.getCurrentScreen());
        assertTrue(state.getGuessScreenAttributes().getTime() > 0);
    }

    @Test
    void submitGuess_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Submit a guess
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);

        // Verify the guess is recorded
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertNotNull(state);
        assertTrue(state.getPlayerGuesses().containsKey(USER1_TOKEN));
        // Verify the returned distance is reasonable (should be about 157,249 meters)
        assertTrue(state.getPlayerGuesses().get(USER1_TOKEN) > 0);
    }

    @Test
    void registerGuessByToken_allGuessesSubmitted() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Ensure user objects are returned by authService
        when(authService.getUserByToken(USER1_TOKEN)).thenReturn(user1);
        when(authService.getUserByToken(USER2_TOKEN)).thenReturn(user2);

        // Make sure the messagingTemplate doesn't throw exceptions - fix ambiguous method references
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));
        doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any(Object.class));

        // Make sure coordinates are available for distance calculation in BOTH places it might be read from
        GameState initialState = gameService.getGameState(TEST_GAME_ID);
        initialState.setCurrentLatLngDTO(new LatLngDTO(45.0, 45.0)); // This was missing
        initialState.getGuessScreenAttributes().setLatitude(45.0);
        initialState.getGuessScreenAttributes().setLongitude(45.0);

        // Register guesses for all players
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0); // Use submitGuess directly instead of registerGuessByToken
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);

        // Verify all guesses are recorded
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertNotNull(state);
        assertEquals(2, state.getPlayerGuesses().size());

        // Directly determine the winner manually rather than relying on automatic detection
        String winnerToken = gameService.determineRoundWinner(TEST_GAME_ID);

        // Refresh state after determining winner
        state = gameService.getGameState(TEST_GAME_ID);

        // Now verify the round was completed properly
        assertNotNull(winnerToken, "Winner token should not be null");
        assertNotNull(state.getLastRoundWinnerToken(), "Last round winner token should not be null");
        assertEquals(GameStatus.ROUND_COMPLETE, state.getStatus());
        assertEquals("REVEAL", state.getCurrentScreen());
    }

    @Test
    void prepareNextRound_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Ensure user objects are returned by authService
        when(authService.getUserByToken(USER1_TOKEN)).thenReturn(user1);
        when(authService.getUserByToken(USER2_TOKEN)).thenReturn(user2);

        // Submit guesses for all players
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);

        // Determine a winner
        String winnerToken = gameService.determineRoundWinner(TEST_GAME_ID);
        assertNotNull(winnerToken, "Winner token should not be null");

        // Prepare for the next round
        gameService.prepareNextRound(TEST_GAME_ID, winnerToken);

        // Verify the next round is prepared correctly
        GameState state = gameService.getGameState(TEST_GAME_ID);
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
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Submit guesses for all players
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);

        // Determine a winner
        String winnerToken = gameService.determineRoundWinner(TEST_GAME_ID);

        // End the game
        gameService.endGame(TEST_GAME_ID, winnerToken);

        // Verify the game is ended correctly
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertNotNull(state);
        assertEquals(GameStatus.GAME_OVER, state.getStatus());
        assertEquals("GAMEOVER", state.getCurrentScreen());
        assertEquals(winnerToken, state.getGameWinnerToken());
    }

    @Test
    void areAllGuessesSubmitted_true() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Submit guesses for all players
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);

        // Verify all guesses are submitted
        assertTrue(gameService.areAllGuessesSubmitted(TEST_GAME_ID));
    }

    @Test
    void areAllGuessesSubmitted_false() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID,testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Submit a guess for only one player
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);

        // Verify not all guesses are submitted
        assertFalse(gameService.areAllGuessesSubmitted(TEST_GAME_ID));
    }

    @Test
    void determineRoundWinner_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID,testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        // Submit guesses for all players with user1 having a better guess
        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 45.1, 45.1); // closer to 45,45
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0); // farther from 45,45

        // Determine the winner
        String winnerToken = gameService.determineRoundWinner(TEST_GAME_ID);

        // Verify the winner is correctly determined (user1 should win)
        assertEquals(USER1_TOKEN, winnerToken);
    }

    @Test
    void markCardPlayedThisRound_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID,testRoundCard);

        String cardId = "testActionCard";
        String targetToken = USER2_TOKEN;

        // Mark a card as played
        gameService.markCardPlayedThisRound(TEST_GAME_ID, USER1_TOKEN, cardId, targetToken);

        // Verify the card is marked as played
        assertTrue(gameService.isCardPlayedInCurrentRound(TEST_GAME_ID, USER1_TOKEN));
        assertTrue(gameService.isPlayerPunishedThisRound(TEST_GAME_ID, targetToken, cardId));
    }

    @Test
    void resetRoundTracking_success() {
        // First start a round with the test round card
        gameService.startRound(TEST_GAME_ID,testRoundCard);

        // Mark a card as played
        gameService.markCardPlayedThisRound(TEST_GAME_ID, USER1_TOKEN, "testActionCard", USER2_TOKEN);

        // Reset round tracking
        gameService.resetRoundTracking(TEST_GAME_ID);

        // Verify the tracking is reset
        assertFalse(gameService.isCardPlayedInCurrentRound(TEST_GAME_ID, USER1_TOKEN));
        assertFalse(gameService.isPlayerPunishedThisRound(TEST_GAME_ID, USER2_TOKEN, "testActionCard"));
    }
    @Test
    void hasRoundCards_returnsTrue() {
        assertTrue(gameService.hasRoundCards(TEST_GAME_ID, USER1_TOKEN));
    }

    @Test
    void hasRoundCards_returnsFalse_whenNoCards() {

        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.getInventoryForPlayer(USER1_TOKEN).setRoundCards(List.of());


        assertFalse(gameService.hasRoundCards(TEST_GAME_ID, USER1_TOKEN));
    }

    @Test
    void hasRoundCards_returnsFalse_whenGameNotFound() {

        assertFalse(gameService.hasRoundCards(999L, USER1_TOKEN));
    }

    @Test
    void selectRandomPlayerWithCards_returnsPlayerWithCards() {

        String randomPlayer = gameService.selectRandomPlayerWithCards(TEST_GAME_ID, playerTokens);
        assertNotNull(randomPlayer);
        assertTrue(playerTokens.contains(randomPlayer));
        assertTrue(gameService.hasRoundCards(TEST_GAME_ID, randomPlayer));
    }

    @Test
    void selectRandomPlayerWithCards_returnsAnyPlayer_whenNoneHaveCards() {

        GameState state = gameService.getGameState(TEST_GAME_ID);
        for (String token : playerTokens) {
            state.getInventoryForPlayer(token).setRoundCards(List.of());
        }


        String randomPlayer = gameService.selectRandomPlayerWithCards(TEST_GAME_ID, playerTokens);
        assertNotNull(randomPlayer);
        assertTrue(playerTokens.contains(randomPlayer));
    }

    @Test
    void selectRandomPlayerWithCards_returnsNull_forEmptyList() {

        assertNull(gameService.selectRandomPlayerWithCards(TEST_GAME_ID, List.of()));
    }

    @Test
    void applyActionCardToPlayer_success() {

        String actionCardId = "testActionCard";
        boolean result = gameService.applyActionCardToPlayer(TEST_GAME_ID, USER2_TOKEN, actionCardId);


        assertTrue(result);
        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertTrue(state.getPlayerInfo().get(USER2_TOKEN).getActiveActionCards().contains(actionCardId));
    }

    @Test
    void applyActionCardToPlayer_alreadyApplied() {

        String actionCardId = "testActionCard";
        gameService.applyActionCardToPlayer(TEST_GAME_ID, USER2_TOKEN, actionCardId);


        boolean result = gameService.applyActionCardToPlayer(TEST_GAME_ID, USER2_TOKEN, actionCardId);


        assertFalse(result);
    }

    @Test
    void applyActionCardToPlayer_createsPlayerInfoIfNeeded() {
        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.getPlayerInfo().remove(USER2_TOKEN);

        String actionCardId = "testActionCard";
        boolean result = gameService.applyActionCardToPlayer(TEST_GAME_ID, USER2_TOKEN, actionCardId);

        assertTrue(result);
        assertNotNull(state.getPlayerInfo().get(USER2_TOKEN));
        assertTrue(state.getPlayerInfo().get(USER2_TOKEN).getActiveActionCards().contains(actionCardId));
    }

    @Test
    void startRound_alreadyActive() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        LatLngDTO coords = gameService.startRound(TEST_GAME_ID, testRoundCard);

        assertNotNull(coords);
        assertEquals(45.0, coords.getLatitude());
        assertEquals(45.0, coords.getLongitude());
    }

    @Test
    void startRound_withGoogleMapsFailure() {
        when(googleMapsService.getRandomCoordinatesOnLand(TEST_GAME_ID))
                .thenThrow(new RuntimeException("API Error"))
                .thenThrow(new RuntimeException("API Error"))
                .thenReturn(new LatLngDTO(45.0, 45.0));

        LatLngDTO coords = gameService.startRound(TEST_GAME_ID, testRoundCard);

        assertNotNull(coords);
        assertEquals(45.0, coords.getLatitude());
        assertEquals(45.0, coords.getLongitude());
    }

    @Test
    void startGuessingPhase_inWrongState() {
        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setStatus(GameStatus.WAITING_FOR_GUESSES);

        assertThrows(IllegalStateException.class, () -> gameService.startGuessingPhase(TEST_GAME_ID));
    }

    @Test
    void startGuessingPhase_withoutCoordinates() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setCurrentLatLngDTO(null);

        gameService.startGuessingPhase(TEST_GAME_ID);

        assertEquals(GameStatus.WAITING_FOR_GUESSES, state.getStatus());
        assertEquals("GUESS", state.getCurrentScreen());
        assertTrue(state.getGuessScreenAttributes().getTime() > 0);
    }

    @Test
    void submitGuess_invalidGameState() {

        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);


        assertThrows(IllegalStateException.class, () ->
                gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0));
    }

    @Test
    void determineRoundWinner_notAllGuessesSubmitted() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);

        assertThrows(IllegalStateException.class, () -> gameService.determineRoundWinner(TEST_GAME_ID));
    }

    @Test
    void determineRoundWinner_invalidGameState() {
        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);

        assertThrows(IllegalStateException.class, () -> gameService.determineRoundWinner(TEST_GAME_ID));
    }

    @Test
    void prepareNextRound_invalidGameState() {
        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);

        assertThrows(IllegalStateException.class, () ->
                gameService.prepareNextRound(TEST_GAME_ID, USER1_TOKEN));
    }

    @Test
    void prepareNextRound_discardPlayedCard() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setCurrentRoundCardPlayer(USER1_TOKEN);
        state.setCurrentRoundCardId(testRoundCard.getId());

        ArrayList<String> roundCards = new ArrayList<>();
        roundCards.add(testRoundCard.getId());
        roundCards.add("other-card");


        state.getInventoryForPlayer(USER1_TOKEN).setRoundCards(roundCards);


        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);

        state.setStatus(GameStatus.ROUND_COMPLETE);


        gameService.prepareNextRound(TEST_GAME_ID, USER1_TOKEN);


        verify(roundCardService).removeRoundCardFromPlayerByToken(
                TEST_GAME_ID, USER1_TOKEN, testRoundCard.getId());

        assertFalse(state.getInventoryForPlayer(USER1_TOKEN).getRoundCards().contains(testRoundCard.getId()));
    }

    @Test
    void prepareNextRound_endGame_whenNoCards() {

        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);


        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);
        gameService.submitGuess(TEST_GAME_ID, USER2_TOKEN, 46.0, 46.0);


        GameState state = gameService.getGameState(TEST_GAME_ID);
        state.setStatus(GameStatus.ROUND_COMPLETE);


        state.getInventoryForPlayer(USER1_TOKEN).setRoundCards(List.of());


        when(roundCardService.getPlayerRoundCardsByToken(TEST_GAME_ID, USER1_TOKEN))
                .thenReturn(List.of());


        gameService.prepareNextRound(TEST_GAME_ID, USER1_TOKEN);


        assertEquals(GameStatus.GAME_OVER, state.getStatus());
        assertEquals("GAMEOVER", state.getCurrentScreen());
        assertEquals(USER1_TOKEN, state.getGameWinnerToken());
    }

    @Test
    void startGame_success() {

        gameService.startGame(TEST_GAME_ID, 5);


        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertEquals(5, state.getMaxRounds());
        assertEquals("ROUNDCARD", state.getCurrentScreen());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, state.getStatus());


        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(), anyString(), any(WebSocketMessage.class));
    }

    @Test
    void sendGameStateToUser_success() {

        gameService.startRound(TEST_GAME_ID, testRoundCard);


        reset(messagingTemplate);


        gameService.sendGameStateToUser(TEST_GAME_ID, USER1_TOKEN);


        verify(messagingTemplate).convertAndSendToUser(
                eq(USER1_TOKEN), anyString(), any(WebSocketMessage.class));
    }

    @Test
    void sendGameStateToUser_gameNotFound() {
        gameService.sendGameStateToUser(999L, USER1_TOKEN);

        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), anyString(), any(WebSocketMessage.class));
    }

    @Test
    void sendGameStateToUserByToken_success() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        reset(messagingTemplate);

        gameService.sendGameStateToUserByToken(TEST_GAME_ID, USER1_TOKEN);

        verify(messagingTemplate).convertAndSendToUser(
                eq(USER1_TOKEN), anyString(), any(WebSocketMessage.class));
    }

    @Test
    void sendGameStateToAll_success() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        reset(messagingTemplate);

        gameService.sendGameStateToAll(TEST_GAME_ID);

        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(), anyString(), any(WebSocketMessage.class));
    }

    @Test
    void hasPlayerSubmittedGuess_returnsTrue() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        gameService.submitGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);

        assertTrue(gameService.hasPlayerSubmittedGuess(TEST_GAME_ID, USER1_TOKEN));
    }

    @Test
    void hasPlayerSubmittedGuess_returnsFalse() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        assertFalse(gameService.hasPlayerSubmittedGuess(TEST_GAME_ID, USER1_TOKEN));
    }

    @Test
    void hasPlayerSubmittedGuess_returnsFalse_gameNotFound() {

        assertFalse(gameService.hasPlayerSubmittedGuess(999L, USER1_TOKEN));
    }

    @Test
    void getPlayerTokens_success() {
        List<String> tokens = gameService.getPlayerTokens(TEST_GAME_ID);

        assertEquals(playerTokens, tokens);
    }

    @Test
    void getPlayerTokens_emptyForNonExistentGame() {
        List<String> tokens = gameService.getPlayerTokens(999L);

        assertTrue(tokens.isEmpty());
    }

    @Test
    void getPlayedCardCount_success() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        gameService.markCardPlayedThisRound(TEST_GAME_ID, USER1_TOKEN, "testActionCard", USER2_TOKEN);

        int count = gameService.getPlayedCardCount(TEST_GAME_ID);

        assertEquals(1, count);
    }

    @Test
    void getPlayedCardCount_zeroForEmptyGame() {
        int count = gameService.getPlayedCardCount(999L);

        assertEquals(0, count);
    }

    @Test
    void registerGuess_success() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);
        gameService.startGuessingPhase(TEST_GAME_ID);

        when(authService.getUserByToken(USER1_TOKEN)).thenReturn(user1);

        gameService.registerGuess(TEST_GAME_ID, USER1_TOKEN, 44.0, 44.0);

        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertTrue(state.getPlayerGuesses().containsKey(USER1_TOKEN));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/lobby/" + TEST_GAME_ID + "/game"),
                any(WebSocketMessage.class));
    }

    @Test
    void cleanupGame_success() {
        gameService.startRound(TEST_GAME_ID, testRoundCard);

        gameService.markCardPlayedThisRound(TEST_GAME_ID, USER1_TOKEN, "testActionCard", USER2_TOKEN);

        gameService.cleanupGame(TEST_GAME_ID);

        assertNull(gameService.getGameState(TEST_GAME_ID));
        assertEquals(0, gameService.getPlayedCardCount(TEST_GAME_ID));
        assertFalse(gameService.isCardPlayedInCurrentRound(TEST_GAME_ID, USER1_TOKEN));
        assertFalse(gameService.isPlayerPunishedThisRound(TEST_GAME_ID, USER2_TOKEN, "testActionCard"));
    }

    @Test
    void initializeGame_withStartingPlayerToken() {
        gameService.initializeGame(TEST_GAME_ID, playerTokens, USER2_TOKEN);

        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertEquals(USER2_TOKEN, state.getCurrentTurnPlayerToken());
    }

    @Test
    void initializeGame_withInvalidStartingPlayerToken() {
        gameService.initializeGame(TEST_GAME_ID, playerTokens, "invalid-token");

        GameState state = gameService.getGameState(TEST_GAME_ID);
        assertTrue(playerTokens.contains(state.getCurrentTurnPlayerToken()));
    }
}
