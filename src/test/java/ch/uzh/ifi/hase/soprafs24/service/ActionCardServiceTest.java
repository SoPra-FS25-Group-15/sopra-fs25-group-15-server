package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ActionCardServiceTest {

    @Autowired
    private ActionCardService actionCardService;
    
    private static final String POWERUP_TYPE = "powerup";
    private static final String PUNISHMENT_TYPE = "punishment";
    
    @Test
    void drawRandomCard_returnsValidCard() {
        // Call the method
        ActionCardDTO card = actionCardService.drawRandomCard();
        
        // Verify that the card is not null and has valid fields
        assertNotNull(card);
        assertNotNull(card.getId());
        assertNotNull(card.getType());
        assertNotNull(card.getTitle());
        assertNotNull(card.getDescription());
        
        // Verify card type is either powerup or punishment
        assertTrue(card.getType().equals(POWERUP_TYPE) || 
                   card.getType().equals(PUNISHMENT_TYPE),
                  "Card type should be either 'powerup' or 'punishment'");
    }
    
    @Test
    void drawRandomCard_validCardIds() {
        // The service should only return cards with specific IDs
        ActionCardDTO card = actionCardService.drawRandomCard();
        
        // Check if the ID is one of the expected values
        assertTrue(card.getId().equals("7choices") || card.getId().equals("badsight"),
                  "Card ID should be either '7choices' or 'badsight'");
        
        // Verify that the ID corresponds to the correct type
        if (card.getId().equals("7choices")) {
            assertEquals(POWERUP_TYPE, card.getType());
            assertEquals("7 Choices", card.getTitle());
            assertTrue(card.getDescription().contains("continent"));
        } else {
            assertEquals(PUNISHMENT_TYPE, card.getType());
            assertEquals("Bad Sight", card.getTitle());
            assertTrue(card.getDescription().contains("blur"));
        }
    }
    
    // This test runs multiple times to ensure we get both types of cards
    @RepeatedTest(10)
    void drawRandomCard_multipleDraws_coversBothTypes() {
        Set<String> foundTypes = new HashSet<>();
        Set<String> foundIds = new HashSet<>();
        
        // Draw cards up to 20 times to increase chance of getting both types
        for (int i = 0; i < 20; i++) {
            ActionCardDTO card = actionCardService.drawRandomCard();
            foundTypes.add(card.getType());
            foundIds.add(card.getId());
            
            // Break early if we've found both types and IDs
            if (foundTypes.size() == 2 && 
                foundTypes.contains(POWERUP_TYPE) && 
                foundTypes.contains(PUNISHMENT_TYPE) &&
                foundIds.contains("7choices") &&
                foundIds.contains("badsight")) {
                break;
            }
        }
        
        // We should have found both types
        assertEquals(2, foundTypes.size(), "Should have found both card types");
        assertTrue(foundTypes.contains(POWERUP_TYPE), "Should have found powerup type");
        assertTrue(foundTypes.contains(PUNISHMENT_TYPE), "Should have found punishment type");
        
        // We should have found both IDs
        assertEquals(2, foundIds.size(), "Should have found both card IDs");
        assertTrue(foundIds.contains("7choices"), "Should have found 7choices card");
        assertTrue(foundIds.contains("badsight"), "Should have found badsight card");
    }
    
    @Test
    void findById_returnsCorrectCard() {
        // Find cards by ID
        ActionCardDTO powerupCard = actionCardService.findById("7choices");
        ActionCardDTO punishmentCard = actionCardService.findById("badsight");
        
        // Verify powerup card
        assertNotNull(powerupCard);
        assertEquals("7choices", powerupCard.getId());
        assertEquals(POWERUP_TYPE, powerupCard.getType());
        
        // Verify punishment card
        assertNotNull(punishmentCard);
        assertEquals("badsight", punishmentCard.getId());
        assertEquals(PUNISHMENT_TYPE, punishmentCard.getType());
        
        // Test with invalid ID
        ActionCardDTO invalidCard = actionCardService.findById("invalid-id");
        assertNull(invalidCard);
    }
    
    @Test
    void isValidActionCard_checksCardExistence() {
        // Verify valid cards
        assertTrue(actionCardService.isValidActionCard("7choices"));
        assertTrue(actionCardService.isValidActionCard("badsight"));
        
        // Verify invalid cards
        assertFalse(actionCardService.isValidActionCard("invalid-id"));
        assertFalse(actionCardService.isValidActionCard(null));
    }
    
    @Test
    void processActionCardEffect_returnsCorrectEffect() {
        // Test 7choices card
        ActionCardEffectDTO powerupEffect = actionCardService.processActionCardEffect("7choices", null);
        assertNotNull(powerupEffect);
        assertEquals("continent", powerupEffect.getEffectType());
        assertNull(powerupEffect.getTargetPlayer());
        
        // Test badsight card
        String targetPlayer = "player-token";
        ActionCardEffectDTO punishmentEffect = actionCardService.processActionCardEffect("badsight", targetPlayer);
        assertNotNull(punishmentEffect);
        assertEquals("blur", punishmentEffect.getEffectType());
        assertEquals(targetPlayer, punishmentEffect.getTargetPlayer());
        
        // Test invalid card ID
        ActionCardEffectDTO invalidEffect = actionCardService.processActionCardEffect("invalid-id", targetPlayer);
        assertNull(invalidEffect);
    }
}
