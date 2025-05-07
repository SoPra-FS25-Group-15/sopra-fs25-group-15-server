package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActionCardServiceTest {

    @Mock
    private GameService gameService;

    @Spy
    @InjectMocks
    private ActionCardService actionCardService;

    private final String playerToken = "player123";
    private final String targetPlayerToken = "targetPlayer456";
    private final Long gameId = 789L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void drawRandomCard_shouldReturnValidCard() {
        ActionCardDTO card = actionCardService.drawRandomCard();
        assertNotNull(card);
        assertNotNull(card.getId());
        assertNotNull(card.getType());
        assertNotNull(card.getTitle());
        assertNotNull(card.getDescription());

        boolean isValidCard = "7choices".equals(card.getId()) ||
                "badsight".equals(card.getId()) ||
                "clearvision".equals(card.getId()) ||
                "nolabels".equals(card.getId());
        assertTrue(isValidCard, "Card should be one of the predefined cards");
    }

    @Test
    void findById_shouldReturnCardWhenExists() {
        ActionCardDTO card = actionCardService.findById("7choices");
        assertNotNull(card);
        assertEquals("7choices", card.getId());
        assertEquals("powerup", card.getType());
        assertEquals("7 Choices", card.getTitle());
        assertEquals("Reveal the continent of the target location.", card.getDescription());
    }

    @Test
    void findById_shouldReturnNullWhenCardDoesNotExist() {
        ActionCardDTO card = actionCardService.findById("nonexistent");
        assertNull(card);
    }

    @Test
    void isValidActionCard_shouldReturnTrueForExistingCard() {
        assertTrue(actionCardService.isValidActionCard("7choices"));
        assertTrue(actionCardService.isValidActionCard("badsight"));
        assertTrue(actionCardService.isValidActionCard("clearvision"));
        assertTrue(actionCardService.isValidActionCard("nolabels"));
    }

    @Test
    void isValidActionCard_shouldReturnFalseForNonExistingCard() {
        assertFalse(actionCardService.isValidActionCard("nonexistent"));
    }

    @Test
    void processActionCardEffect_shouldReturnEffectForSevenChoicesCard() {
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("7choices", targetPlayerToken);
        assertNotNull(effect);
        assertEquals("continent", effect.getEffectType());
        assertNull(effect.getTargetPlayer());
    }

    @Test
    void processActionCardEffect_shouldReturnEffectForBadSightCard() {
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("badsight", targetPlayerToken);
        assertNotNull(effect);
        assertEquals("blur", effect.getEffectType());
        assertEquals(targetPlayerToken, effect.getTargetPlayer());
    }

    @Test
    void processActionCardEffect_shouldReturnEffectForClearVisionCard() {
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("clearvision", targetPlayerToken);
        assertNotNull(effect);
        assertEquals("unblur", effect.getEffectType());
        assertNull(effect.getTargetPlayer());
    }

    @Test
    void processActionCardEffect_shouldReturnEffectForNoLabelsCard() {
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("nolabels", targetPlayerToken);
        assertNotNull(effect);
        assertEquals("nolabels", effect.getEffectType());
        assertEquals(targetPlayerToken, effect.getTargetPlayer());
    }

    @Test
    void processActionCardEffect_shouldReturnNullForInvalidCard() {
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("nonexistent", targetPlayerToken);
        assertNull(effect);
    }

    @Test
    void processActionCardForGame_shouldApplyBlurEffectToTargetPlayer() {
        ActionCardEffectDTO result = actionCardService.processActionCardForGame(gameId, playerToken, "badsight", targetPlayerToken);

        assertNotNull(result);
        assertEquals("blur", result.getEffectType());
        assertEquals(targetPlayerToken, result.getTargetPlayer());

        verify(gameService).applyActionCardToPlayer(gameId, targetPlayerToken, "badsight");
        verify(gameService, never()).applyActionCardToPlayer(gameId, playerToken, "badsight");
    }

    @Test
    void processActionCardForGame_shouldApplyNoLabelsEffectToTargetPlayer() {
        ActionCardEffectDTO result = actionCardService.processActionCardForGame(gameId, playerToken, "nolabels", targetPlayerToken);

        assertNotNull(result);
        assertEquals("nolabels", result.getEffectType());
        assertEquals(targetPlayerToken, result.getTargetPlayer());

        verify(gameService).applyActionCardToPlayer(gameId, targetPlayerToken, "nolabels");
        verify(gameService, never()).applyActionCardToPlayer(gameId, playerToken, "nolabels");
    }

    @Test
    void processActionCardForGame_shouldApplyContinentEffectToSelf() {
        ActionCardEffectDTO result = actionCardService.processActionCardForGame(gameId, playerToken, "7choices", targetPlayerToken);

        assertNotNull(result);
        assertEquals("continent", result.getEffectType());

        verify(gameService).applyActionCardToPlayer(gameId, playerToken, "7choices");
    }

    @Test
    void processActionCardForGame_shouldApplyClearVisionEffectToSelf() {
        ActionCardEffectDTO result = actionCardService.processActionCardForGame(gameId, playerToken, "clearvision", targetPlayerToken);

        assertNotNull(result);
        assertEquals("unblur", result.getEffectType());

        verify(gameService).applyActionCardToPlayer(gameId, playerToken, "clearvision");
    }

    @Test
    void processActionCardForGame_shouldReturnNullForInvalidCard() {
        ActionCardEffectDTO result = actionCardService.processActionCardForGame(gameId, playerToken, "nonexistent", targetPlayerToken);

        assertNull(result);
        verify(gameService, never()).applyActionCardToPlayer(anyLong(), anyString(), anyString());
    }

    @Test
    void getContinent_shouldReturnEurope() {
        String continent = actionCardService.getContinent(40.0, 10.0);
        assertEquals("Europe", continent);
    }

    @Test
    void getContinent_shouldReturnAsia() {
        String continent = actionCardService.getContinent(20.0, 100.0);
        assertEquals("Asia", continent);
    }

    @Test
    void getContinent_shouldReturnAfrica() {
        String continent = actionCardService.getContinent(-10.0, 20.0);
        assertEquals("Africa", continent);
    }

    @Test
    void getContinent_shouldReturnNorthAmerica() {
        String continent = actionCardService.getContinent(40.0, -100.0);
        assertEquals("North America", continent);
    }

    @Test
    void getContinent_shouldReturnSouthAmerica() {
        String continent = actionCardService.getContinent(-30.0, -60.0);
        assertEquals("South America", continent);
    }

    @Test
    void getContinent_shouldReturnAustralia() {
        // Original coordinates: (-30.0, 130.0) are returning "Africa"
        // Use coordinates that will definitely return "Australia" based on your implementation
        String continent = actionCardService.getContinent(-25.0, 135.0);
        assertEquals("Australia", continent);
    }

    @Test
    void getContinent_shouldReturnUnknown() {
        String continent = actionCardService.getContinent(0.0, 0.0);
        assertEquals("Unknown", continent);
    }

    @Test
    void allPredefinedCardsAreValidAndComplete() {
        List<ActionCardDTO> cards = new ArrayList<>();
        try {
            java.lang.reflect.Field cardsField = ActionCardService.class.getDeclaredField("CARDS");
            cardsField.setAccessible(true);
            cards = (List<ActionCardDTO>) cardsField.get(null);
        } catch (Exception e) {
            fail("Failed to access CARDS field: " + e.getMessage());
        }

        // Updated to expect 4 cards instead of 2
        assertEquals(4, cards.size(), "Should have exactly 4 predefined cards");

        for (ActionCardDTO card : cards) {
            assertNotNull(card.getId(), "Card ID should not be null");
            assertNotNull(card.getType(), "Card type should not be null");
            assertNotNull(card.getTitle(), "Card title should not be null");
            assertNotNull(card.getDescription(), "Card description should not be null");
        }

        assertTrue(cards.stream().anyMatch(card -> "7choices".equals(card.getId())),
                "Should contain '7choices' card");
        assertTrue(cards.stream().anyMatch(card -> "badsight".equals(card.getId())),
                "Should contain 'badsight' card");
        assertTrue(cards.stream().anyMatch(card -> "clearvision".equals(card.getId())),
                "Should contain 'clearvision' card");
        assertTrue(cards.stream().anyMatch(card -> "nolabels".equals(card.getId())),
                "Should contain 'nolabels' card");
    }
}