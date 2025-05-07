package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameStatus;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private GameState gameState;

    @Mock
    private RoundCardDTO mockRoundCard;

    @Mock
    private LatLngDTO mockLatLngDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameState = new GameState();

        
        List<String> playerTokens = Arrays.asList("token1", "token2", "token3");
        gameState.setPlayerTokens(playerTokens);
        gameState.setCurrentTurnPlayerToken("token1");
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
    }

    @Test
    void testInitialState() {
        
        assertEquals(0, gameState.getCurrentRound());
        assertEquals("ROUNDCARD", gameState.getCurrentScreen());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, gameState.getStatus());
        assertNull(gameState.getGameWinnerToken());
        assertEquals(5, gameState.getMaxRounds()); 
    }

    @Test
    void testPlayerTokens() {
        List<String> tokens = Arrays.asList("token1", "token2", "token3");
        gameState.setPlayerTokens(tokens);
        assertEquals(tokens, gameState.getPlayerTokens());
        assertEquals(3, gameState.getPlayerTokens().size());
    }

    @Test
    void testCurrentTurnPlayerToken() {
        String token = "token1";
        gameState.setCurrentTurnPlayerToken(token);
        assertEquals(token, gameState.getCurrentTurnPlayerToken());
    }

    @Test
    void testRoundCardAndCoordinates() {
        gameState.setCurrentRoundCard(mockRoundCard);
        gameState.setCurrentLatLngDTO(mockLatLngDTO);

        assertEquals(mockRoundCard, gameState.getCurrentRoundCard());
        assertEquals(mockLatLngDTO, gameState.getCurrentLatLngDTO());
    }

    @Test
    void testActiveRoundCard() {
        String cardId = "card123";
        gameState.setActiveRoundCard(cardId);
        assertEquals(cardId, gameState.getActiveRoundCard());
    }

    @Test
    void testCurrentRound() {
        gameState.setCurrentRound(3);
        assertEquals(3, gameState.getCurrentRound());
    }

    @Test
    void testMaxRounds() {
        gameState.setMaxRounds(10);
        assertEquals(10, gameState.getMaxRounds());
    }

    @Test
    void testGameStatus() {
        gameState.setStatus(GameStatus.WAITING_FOR_GUESSES);
        assertEquals(GameStatus.WAITING_FOR_GUESSES, gameState.getStatus());

        gameState.setStatus(GameStatus.ROUND_COMPLETE);
        assertEquals(GameStatus.ROUND_COMPLETE, gameState.getStatus());
    }

    @Test
    void testCurrentScreen() {
        gameState.setCurrentScreen("GUESS");
        assertEquals("GUESS", gameState.getCurrentScreen());

        gameState.setCurrentScreen("REVEAL");
        assertEquals("REVEAL", gameState.getCurrentScreen());
    }

    @Test
    void testPlayerGuesses() {
        assertTrue(gameState.getPlayerGuesses().isEmpty());

        gameState.getPlayerGuesses().put("token1", 1000);
        gameState.getPlayerGuesses().put("token2", 2000);

        assertEquals(2, gameState.getPlayerGuesses().size());
        assertEquals(Integer.valueOf(1000), gameState.getPlayerGuesses().get("token1"));
        assertEquals(Integer.valueOf(2000), gameState.getPlayerGuesses().get("token2"));
    }

    @Test
    void testWinnerTokens() {
        gameState.setLastRoundWinnerToken("token2");
        assertEquals("token2", gameState.getLastRoundWinnerToken());

        gameState.setGameWinnerToken("token3");
        assertEquals("token3", gameState.getGameWinnerToken());
    }

    @Test
    void testLastRoundWinningDistance() {
        gameState.setLastRoundWinningDistance(5000);
        assertEquals(5000, gameState.getLastRoundWinningDistance());
    }

    @Test
    void testRoundCardSubmitter() {
        gameState.setRoundCardSubmitter("Player 1");
        assertEquals("Player 1", gameState.getRoundCardSubmitter());
    }

    @Test
    void testCurrentRoundCardTracking() {
        gameState.setCurrentRoundCardPlayer("token1");
        gameState.setCurrentRoundCardId("card123");

        assertEquals("token1", gameState.getCurrentRoundCardPlayer());
        assertEquals("card123", gameState.getCurrentRoundCardId());
    }

    @Test
    void testPlayerInventoryInitialization() {
        assertNotNull(gameState.getInventoryForPlayer("token1"));
        assertTrue(gameState.getInventoryForPlayer("token1").getRoundCards().isEmpty());
        assertTrue(gameState.getInventoryForPlayer("token1").getActionCards().isEmpty());

        assertEquals(1, gameState.getInventories().size());

        assertNotNull(gameState.getInventoryForPlayer("token2"));
        assertEquals(2, gameState.getInventories().size());
    }

    @Test
    void testPlayerInventoryCardManagement() {
        GameState.PlayerInventory inventory = gameState.getInventoryForPlayer("token1");

        List<String> roundCards = Arrays.asList("round1", "round2", "round3");
        inventory.setRoundCards(roundCards);
        assertEquals(roundCards, inventory.getRoundCards());
        assertEquals(3, inventory.getRoundCards().size());

        List<String> actionCards = Arrays.asList("action1", "action2");
        inventory.setActionCards(actionCards);
        assertEquals(actionCards, inventory.getActionCards());
        assertEquals(2, inventory.getActionCards().size());
    }

    @Test
    void testGuessScreenAttributes() {
        GameState.GuessScreenAttributes attrs = gameState.getGuessScreenAttributes();

        assertEquals(0, attrs.getTime());
        assertEquals(0.0, attrs.getLatitude());
        assertEquals(0.0, attrs.getLongitude());
        assertNull(attrs.getResolveResponse());

        attrs.setTime(30);
        attrs.setLatitude(40.7128);
        attrs.setLongitude(-74.0060);

        assertEquals(30, attrs.getTime());
        assertEquals(40.7128, attrs.getLatitude());
        assertEquals(-74.0060, attrs.getLongitude());

        GameState.ResolveResponse response = new GameState.ResolveResponse();
        response.setId("action1");
        response.setMessage("You've been blocked!");

        attrs.setResolveResponse(response);
        assertEquals(response, attrs.getResolveResponse());
        assertEquals("action1", attrs.getResolveResponse().getId());
        assertEquals("You've been blocked!", attrs.getResolveResponse().getMessage());

        GameState.GuessScreenAttributes newAttrs = new GameState.GuessScreenAttributes();
        newAttrs.setTime(60);
        gameState.setGuessScreenAttributes(newAttrs);
        assertEquals(60, gameState.getGuessScreenAttributes().getTime());
    }

    @Test
    void testPlayerInfoInitialization() {
        assertNotNull(gameState.getPlayerInfo("token1"));
        assertNull(gameState.getPlayerInfo("token1").getUsername());
        assertEquals(0, gameState.getPlayerInfo("token1").getRoundCardsLeft());
        assertEquals(0, gameState.getPlayerInfo("token1").getActionCardsLeft());
        assertTrue(gameState.getPlayerInfo("token1").getActiveActionCards().isEmpty());

        assertEquals(1, gameState.getPlayerInfo().size());

        assertNotNull(gameState.getPlayerInfo("token2"));
        assertEquals(2, gameState.getPlayerInfo().size());
    }

    @Test
    void testPlayerInfoProperties() {
        GameState.PlayerInfo info = gameState.getPlayerInfo("token1");

        
        info.setUsername("Player 1");
        info.setRoundCardsLeft(5);
        info.setActionCardsLeft(3);

        assertEquals("Player 1", info.getUsername());
        assertEquals(5, info.getRoundCardsLeft());
        assertEquals(3, info.getActionCardsLeft());

        
        List<String> activeCards = Arrays.asList("action1", "action2");
        info.setActiveActionCards(activeCards);
        assertEquals(activeCards, info.getActiveActionCards());
        assertEquals(2, info.getActiveActionCards().size());
    }

    @Test
    void testResolveResponse() {
        GameState.ResolveResponse response = new GameState.ResolveResponse();

        response.setId("action123");
        response.setMessage("Your guess has been scrambled!");

        assertEquals("action123", response.getId());
        assertEquals("Your guess has been scrambled!", response.getMessage());
    }

    @Test
    void testPersistenceOfMultiplePlayerData() {
        

        
        gameState.getPlayerInfo("token1").setUsername("Player 1");
        gameState.getPlayerInfo("token1").setRoundCardsLeft(4);
        gameState.getPlayerInfo("token1").setActionCardsLeft(2);
        gameState.getInventoryForPlayer("token1").setRoundCards(Arrays.asList("round1", "round2", "round3", "round4"));
        gameState.getInventoryForPlayer("token1").setActionCards(Arrays.asList("action1", "action2"));

        
        gameState.getPlayerInfo("token2").setUsername("Player 2");
        gameState.getPlayerInfo("token2").setRoundCardsLeft(3);
        gameState.getPlayerInfo("token2").setActionCardsLeft(3);
        gameState.getInventoryForPlayer("token2").setRoundCards(Arrays.asList("round5", "round6", "round7"));
        gameState.getInventoryForPlayer("token2").setActionCards(Arrays.asList("action3", "action4", "action5"));

        
        gameState.getPlayerInfo("token3").setUsername("Player 3");
        gameState.getPlayerInfo("token3").setRoundCardsLeft(5);
        gameState.getPlayerInfo("token3").setActionCardsLeft(1);
        gameState.getInventoryForPlayer("token3").setRoundCards(Arrays.asList("round8", "round9", "round10", "round11", "round12"));
        gameState.getInventoryForPlayer("token3").setActionCards(Arrays.asList("action6"));

        
        assertEquals(3, gameState.getPlayerInfo().size());
        assertEquals(3, gameState.getInventories().size());

        
        assertEquals("Player 1", gameState.getPlayerInfo("token1").getUsername());
        assertEquals(4, gameState.getPlayerInfo("token1").getRoundCardsLeft());
        assertEquals(2, gameState.getPlayerInfo("token1").getActionCardsLeft());
        assertEquals(4, gameState.getInventoryForPlayer("token1").getRoundCards().size());
        assertEquals(2, gameState.getInventoryForPlayer("token1").getActionCards().size());

        
        assertEquals("Player 2", gameState.getPlayerInfo("token2").getUsername());
        assertEquals(3, gameState.getPlayerInfo("token2").getRoundCardsLeft());
        assertEquals(3, gameState.getPlayerInfo("token2").getActionCardsLeft());
        assertEquals(3, gameState.getInventoryForPlayer("token2").getRoundCards().size());
        assertEquals(3, gameState.getInventoryForPlayer("token2").getActionCards().size());

        
        assertEquals("Player 3", gameState.getPlayerInfo("token3").getUsername());
        assertEquals(5, gameState.getPlayerInfo("token3").getRoundCardsLeft());
        assertEquals(1, gameState.getPlayerInfo("token3").getActionCardsLeft());
        assertEquals(5, gameState.getInventoryForPlayer("token3").getRoundCards().size());
        assertEquals(1, gameState.getInventoryForPlayer("token3").getActionCards().size());
    }

    @Test
    void testSequentialRoundTransitions() {
        
        assertEquals(0, gameState.getCurrentRound());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, gameState.getStatus());

        
        gameState.setCurrentRound(1);
        gameState.setCurrentRoundCard(mockRoundCard);
        gameState.setActiveRoundCard("round1");
        gameState.setCurrentRoundCardPlayer("token1");
        gameState.setCurrentRoundCardId("round1");

        
        gameState.setStatus(GameStatus.WAITING_FOR_ACTION_CARDS);
        gameState.setCurrentScreen("ACTIONCARD");

        
        gameState.setStatus(GameStatus.WAITING_FOR_GUESSES);
        gameState.setCurrentScreen("GUESS");
        gameState.setCurrentLatLngDTO(mockLatLngDTO);
        gameState.getGuessScreenAttributes().setTime(30);
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);

        
        gameState.getPlayerGuesses().put("token1", 1000);
        gameState.getPlayerGuesses().put("token2", 2000);
        gameState.getPlayerGuesses().put("token3", 3000);

        
        gameState.setStatus(GameStatus.ROUND_COMPLETE);
        gameState.setCurrentScreen("REVEAL");
        gameState.setLastRoundWinnerToken("token1");
        gameState.setLastRoundWinningDistance(1000);

        
        assertEquals(1, gameState.getCurrentRound());
        assertEquals(GameStatus.ROUND_COMPLETE, gameState.getStatus());
        assertEquals("REVEAL", gameState.getCurrentScreen());
        assertEquals("token1", gameState.getLastRoundWinnerToken());
        assertEquals(1000, gameState.getLastRoundWinningDistance());
        assertEquals(3, gameState.getPlayerGuesses().size());

        
        gameState.setCurrentRound(2);
        gameState.getPlayerGuesses().clear();
        gameState.setCurrentRoundCard(null);
        gameState.setCurrentLatLngDTO(null);
        gameState.setActiveRoundCard(null);
        gameState.setStatus(GameStatus.WAITING_FOR_ROUND_CARD);
        gameState.setCurrentScreen("ROUNDCARD");
        gameState.setCurrentTurnPlayerToken("token1"); 

        
        assertEquals(2, gameState.getCurrentRound());
        assertEquals(GameStatus.WAITING_FOR_ROUND_CARD, gameState.getStatus());
        assertEquals("ROUNDCARD", gameState.getCurrentScreen());
        assertEquals("token1", gameState.getCurrentTurnPlayerToken());
        assertTrue(gameState.getPlayerGuesses().isEmpty());
        assertNull(gameState.getCurrentRoundCard());
        assertNull(gameState.getCurrentLatLngDTO());
        assertNull(gameState.getActiveRoundCard());
    }

    @Test
    void testGameCompletionFlow() {
        
        gameState.setMaxRounds(3);
        gameState.setCurrentRound(3);

        
        gameState.getPlayerGuesses().put("token1", 1000);
        gameState.getPlayerGuesses().put("token2", 2000);
        gameState.getPlayerGuesses().put("token3", 500);

        
        gameState.setLastRoundWinnerToken("token3");
        gameState.setLastRoundWinningDistance(500);

        
        gameState.setStatus(GameStatus.GAME_OVER);
        gameState.setCurrentScreen("GAMEOVER");
        gameState.setGameWinnerToken("token3");

        
        assertEquals(3, gameState.getCurrentRound());
        assertEquals(GameStatus.GAME_OVER, gameState.getStatus());
        assertEquals("GAMEOVER", gameState.getCurrentScreen());
        assertEquals("token3", gameState.getGameWinnerToken());
    }
}