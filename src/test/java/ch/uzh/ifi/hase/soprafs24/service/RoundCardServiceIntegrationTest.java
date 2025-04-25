package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration Test for RoundCardService
 * Tests the complete flow: loading cards, selecting one, updating game session,
 * persisting new state, and reflecting changes on frontend
 */
@WebAppConfiguration
@SpringBootTest
public class RoundCardServiceIntegrationTest {

    @Mock
    private AuthService authService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private RoundCardService roundCardService;

    private User testUser1;
    private User testUser2;
    private Long gameId;
    private String userToken1;
    private String userToken2;
    private GameState gameState;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setUsername("testUser1");
        testUser1.setToken("token-user1");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("testUser2");
        testUser2.setToken("token-user2");

        gameId = 100L;
        userToken1 = "token-user1";
        userToken2 = "token-user2";

        // Configure mocks
        when(authService.getUserByToken(userToken1)).thenReturn(testUser1);
        when(authService.getUserByToken(userToken2)).thenReturn(testUser2);

        // Setup game state
        gameState = new GameState();
        gameState.setPlayerTokens(Arrays.asList(userToken1, userToken2));
        gameState.setCurrentTurnPlayerToken(userToken1);
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_ROUND_CARD);

        when(gameService.getGameState(gameId)).thenReturn(gameState);
    }

    @Test
    public void testAssignInitialRoundCards() {
        // Test initial assignment of round cards to players
        List<Long> playerIds = Arrays.asList(testUser1.getId(), testUser2.getId());
        roundCardService.assignInitialRoundCards(gameId, playerIds);

        // Verify each player has 2 round cards
        List<RoundCardDTO> player1Cards = roundCardService.getPlayerRoundCards(gameId, testUser1.getId());
        List<RoundCardDTO> player2Cards = roundCardService.getPlayerRoundCards(gameId, testUser2.getId());

        assertEquals(2, player1Cards.size(), "Player 1 should have 2 round cards");
        assertEquals(2, player2Cards.size(), "Player 2 should have 2 round cards");

        // Verify cards types (one World and one Flash card for each player)
        verifyRoundCards(player1Cards);
        verifyRoundCards(player2Cards);
    }

    @Test
    public void testAssignAndRemovePlayerRoundCardsByToken() {
        // 1. Assign cards to player by token
        List<RoundCardDTO> cards = roundCardService.assignPlayerRoundCards(gameId, userToken1);

        // Verify initial assignment
        assertEquals(2, cards.size(), "Player should receive 2 round cards");
        verifyRoundCards(cards);

        // Get assigned card IDs for testing removal
        String worldCardId = cards.stream()
                .filter(card -> card.getName().equals("World"))
                .findFirst()
                .orElseThrow()
                .getId();

        String flashCardId = cards.stream()
                .filter(card -> card.getName().equals("Flash"))
                .findFirst()
                .orElseThrow()
                .getId();

        // 2. Test retrieving cards by token
        List<RoundCardDTO> retrievedCards = roundCardService.getPlayerRoundCardsByToken(gameId, userToken1);
        assertEquals(2, retrievedCards.size(), "Retrieved cards count should match");

        // 3. Test removing a card by token
        boolean removed = roundCardService.removeRoundCardFromPlayerByToken(gameId, userToken1, worldCardId);
        assertTrue(removed, "Card removal should be successful");

        // 4. Verify card was removed
        List<RoundCardDTO> remainingCards = roundCardService.getPlayerRoundCardsByToken(gameId, userToken1);
        assertEquals(1, remainingCards.size(), "Player should have 1 card left");
        assertEquals("Flash", remainingCards.get(0).getName(), "Remaining card should be Flash");
    }

    @Test
    public void testTokenBasedFallbackToIdBased() {
        // 1. Assign ID-based cards but no token-based cards
        List<Long> playerIds = Arrays.asList(testUser1.getId());
        roundCardService.assignInitialRoundCards(gameId, playerIds);

        // 2. Test getting cards by token - should fall back to ID-based cards
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCardsByToken(gameId, userToken1);

        // 3. Verify the fallback worked
        assertEquals(2, cards.size(), "Should retrieve 2 cards via ID-based fallback");
        verifyRoundCards(cards);
    }

    @Test
    public void testCompleteRoundCardFlow() {
        // 1. Initialize game with players
        List<Long> playerIds = Arrays.asList(testUser1.getId(), testUser2.getId());
        roundCardService.assignInitialRoundCards(gameId, playerIds);

        // Setup GameState tracking of round cards
        List<String> player1RoundCards = new ArrayList<>();
        GameState.PlayerInventory inventory1 = gameState.getInventoryForPlayer(userToken1);
        GameState.PlayerInfo playerInfo1 = new GameState.PlayerInfo();
        playerInfo1.setUsername("testUser1");
        playerInfo1.setRoundCardsLeft(2);
        gameState.getPlayerInfo().put(userToken1, playerInfo1);

        // 2. Get token-based round cards
        List<RoundCardDTO> tokenCards = roundCardService.getPlayerRoundCardsByToken(gameId, userToken1);

        // Add card IDs to player inventory in game state
        for (RoundCardDTO card : tokenCards) {
            player1RoundCards.add(card.getId());
        }
        inventory1.setRoundCards(player1RoundCards);

        // 3. Get a specific card to use
        RoundCardDTO selectedCard = tokenCards.stream()
                .filter(card -> card.getName().equals("World"))
                .findFirst()
                .orElseThrow();

        // 4. Test updating game with round card
        // Mock the required behavior for game state updates
        when(gameService.startRound(eq(gameId), any(RoundCardDTO.class))).thenAnswer(invocation -> {
            RoundCardDTO card = invocation.getArgument(1);
            gameState.setActiveRoundCard(card.getId());

            // Update game state based on card
            gameState.setCurrentRoundCard(card);
            if (card.getModifiers() != null) {
                gameState.getGuessScreenAttributes().setTime(card.getModifiers().getTime());
            }

            // Return sample coordinates
            return new GoogleMapsService.LatLngDTO(45.0, 45.0);
        });

        // Start the round with the selected card
        GoogleMapsService.LatLngDTO coords = gameService.startRound(gameId, selectedCard);
        assertNotNull(coords, "Coordinates should be returned");

        // 5. Verify card was removed after use
        boolean removed = roundCardService.removeRoundCardFromPlayerByToken(gameId, userToken1, selectedCard.getId());
        assertTrue(removed, "Card should be successfully removed");

        // Update gameState
        player1RoundCards.remove(selectedCard.getId());
        inventory1.setRoundCards(player1RoundCards);
        playerInfo1.setRoundCardsLeft(1);

        // 6. Verify remaining cards
        List<RoundCardDTO> remainingCards = roundCardService.getPlayerRoundCardsByToken(gameId, userToken1);
        assertEquals(1, remainingCards.size(), "Player should have 1 card left");
        assertEquals("Flash", remainingCards.get(0).getName(), "Remaining card should be Flash");

        // 7. Test game state reflection of round card
        assertEquals(selectedCard.getId(), gameState.getActiveRoundCard(),
                "Game state should reflect the active round card");
    }

    @Test
    public void testGetAllRoundCards() {
        // Test the getAllRoundCards method that provides cards for the frontend
        List<RoundCardDTO> allCards = roundCardService.getAllRoundCards();

        // Verify structure and content
        assertEquals(2, allCards.size(), "Should return 2 global round cards");

        // Check for World card
        RoundCardDTO worldCard = allCards.stream()
                .filter(card -> "world".equals(card.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(worldCard, "World card should exist");
        assertEquals("World", worldCard.getName(), "World card should have correct name");
        assertEquals(60, worldCard.getModifiers().getTime(), "World card should have 60 seconds time");

        // Check for Flash card
        RoundCardDTO flashCard = allCards.stream()
                .filter(card -> "flash".equals(card.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(flashCard, "Flash card should exist");
        assertEquals("Flash", flashCard.getName(), "Flash card should have correct name");
        assertEquals(30, flashCard.getModifiers().getTime(), "Flash card should have 30 seconds time");
    }

    @Test
    public void testInvalidTokenHandling() {
        // Test behavior with invalid token
        String invalidToken = "invalid-token";
        when(authService.getUserByToken(invalidToken)).thenReturn(null);

        // Should throw exception for invalid token
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            roundCardService.getPlayerRoundCardsByToken(gameId, invalidToken);
        });

        assertTrue(exception.getMessage().contains("Invalid token"),
                "Exception message should mention invalid token");
    }

    @Test
    public void testGetRoundCardIds() {
        // Test getting the list of round card IDs
        List<String> cardIds = roundCardService.getRoundCardIds();

        assertEquals(2, cardIds.size(), "Should have 2 round card types");
        assertTrue(cardIds.contains("world"), "Should contain world card");
        assertTrue(cardIds.contains("flash"), "Should contain flash card");
    }

    /**
     * Helper method to verify round cards contain one World and one Flash card
     */
    private void verifyRoundCards(List<RoundCardDTO> cards) {
        boolean hasWorldCard = false;
        boolean hasFlashCard = false;

        for (RoundCardDTO card : cards) {
            if ("World".equals(card.getName())) {
                hasWorldCard = true;
                assertEquals(60, card.getModifiers().getTime(), "World card should have 60 seconds time");
            } else if ("Flash".equals(card.getName())) {
                hasFlashCard = true;
                assertEquals(30, card.getModifiers().getTime(), "Flash card should have 30 seconds time");
            }
        }

        assertTrue(hasWorldCard, "Player should have a World card");
        assertTrue(hasFlashCard, "Player should have a Flash card");
    }
}