package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoundCardServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private RoundCardService roundCardService;

    private User testUser;
    private final String TEST_TOKEN = "test-token";
    private final Long TEST_GAME_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(TEST_TOKEN);
        
        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(testUser);
    }

    @Test
    void assignInitialRoundCards_success() {
        List<Long> playerIds = Arrays.asList(1L, 2L);

        roundCardService.assignInitialRoundCards(TEST_GAME_ID, playerIds);

        // Fix: Update the cast to match the actual type (nested map)
        Map<Long, Map<Long, List<RoundCardDTO>>> cards = (Map<Long, Map<Long, List<RoundCardDTO>>>) ReflectionTestUtils
                .getField(roundCardService, "playerRoundCards");
        
        assertNotNull(cards);
        assertTrue(cards.containsKey(TEST_GAME_ID));
        
        Map<Long, List<RoundCardDTO>> gameCards = cards.get(TEST_GAME_ID);
        assertEquals(2, gameCards.size()); // Two players
        
        // Verify each player has the right cards
        for (Long playerId : playerIds) {
            assertTrue(gameCards.containsKey(playerId));
            
            List<RoundCardDTO> playerCards = gameCards.get(playerId);
            assertEquals(2, playerCards.size()); // Each player should have 2 cards

            // Verify card types and IDs
            boolean hasWorld = playerCards.stream().anyMatch(card -> card.getId().startsWith("world-"));
            boolean hasFlash = playerCards.stream().anyMatch(card -> card.getId().startsWith("flash-"));
            
            assertTrue(hasWorld, "Player should have a world card");
            assertTrue(hasFlash, "Player should have a flash card");
        }
    }

    @Test
    void getPlayerRoundCards_success() {
        // Setup test data
        Long playerId = 1L;
        List<Long> playerIds = List.of(playerId);
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, playerIds);

        // Test retrieving cards
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);
        
        assertNotNull(cards);
        assertEquals(2, cards.size());
    }

    @Test
    void getPlayerRoundCards_noneAssigned() {
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, 999L);
        
        assertNotNull(cards);
        assertTrue(cards.isEmpty());
    }

    @Test
    void removeRoundCardFromPlayer_success() {
        // Setup test data
        Long playerId = 1L;
        List<Long> playerIds = List.of(playerId);
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, playerIds);
        
        // Get card IDs
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);
        String cardId = cards.get(0).getId();
        
        // Test removing a card
        boolean removed = roundCardService.removeRoundCardFromPlayer(TEST_GAME_ID, playerId, cardId);
        
        assertTrue(removed);
        
        // Verify card was removed
        List<RoundCardDTO> remainingCards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);
        assertEquals(1, remainingCards.size());
        assertFalse(remainingCards.stream().anyMatch(c -> c.getId().equals(cardId)));
    }

    @Test
    void removeRoundCardFromPlayer_cardNotFound() {
        // Setup test data
        Long playerId = 1L;
        List<Long> playerIds = List.of(playerId);
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, playerIds);
        
        // Test removing a non-existent card
        boolean removed = roundCardService.removeRoundCardFromPlayer(TEST_GAME_ID, playerId, "non-existent-card");
        
        assertFalse(removed);
    }
    
    @Test
    void removeRoundCardFromPlayerByToken_success() {
        // Setup token-based cards
        List<RoundCardDTO> cards = roundCardService.assignPlayerRoundCards(TEST_GAME_ID, TEST_TOKEN);
        String cardId = cards.get(0).getId();
        
        // Test removing a card by token
        boolean removed = roundCardService.removeRoundCardFromPlayerByToken(TEST_GAME_ID, TEST_TOKEN, cardId);
        
        assertTrue(removed);
    }

    @Test
    void assignRoundCardsToPlayer_success() {
        List<RoundCardDTO> cards = roundCardService.assignRoundCardsToPlayer(TEST_TOKEN);
        
        assertEquals(2, cards.size());
        assertTrue(cards.stream().anyMatch(c -> c.getName().equals("World")));
        assertTrue(cards.stream().anyMatch(c -> c.getName().equals("Flash")));
    }

    @Test
    void hasNoRoundCardsByToken_true() {
        // No cards assigned yet
        assertTrue(roundCardService.hasNoRoundCardsByToken(999L, TEST_TOKEN));
    }
    
    @Test
    void getAllRoundCards_success() {
        List<RoundCardDTO> cards = roundCardService.getAllRoundCards();
        
        assertEquals(2, cards.size());
        assertTrue(cards.stream().anyMatch(c -> c.getId().equals("world")));
        assertTrue(cards.stream().anyMatch(c -> c.getId().equals("flash")));
    }

    @Test
    void getRoundCardIds_success() {
        List<String> cardIds = roundCardService.getRoundCardIds();
        
        assertEquals(2, cardIds.size());
        assertTrue(cardIds.contains("world"));
        assertTrue(cardIds.contains("flash"));
    }
}
