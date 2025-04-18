package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActionCardServiceTest {

    @InjectMocks
    private ActionCardService actionCardService;

    @BeforeEach
    public void setup() {
        actionCardService = new ActionCardService();
    }

    @Test
    void drawRandomCard_returnValidCard() {
        // Call the method
        ActionCardDTO card = actionCardService.drawRandomCard();
        
        // Check that it returns a valid card
        assertNotNull(card);
        assertNotNull(card.getId());
        assertNotNull(card.getType());
        assertNotNull(card.getTitle());
        assertNotNull(card.getDescription());
        
        // Check that the type is either "powerup" or "punishment"
        assertTrue(card.getType().equals("powerup") || card.getType().equals("punishment"));
    }
    
    @Test
    void drawRandomCard_bothCardsCanBeDrawn() {
        // We'll call drawRandomCard many times and verify we get both cards
        Set<String> drawnCardIds = new HashSet<>();
        
        // With 100 draws, we should get both cards with very high probability
        for (int i = 0; i < 100; i++) {
            ActionCardDTO card = actionCardService.drawRandomCard();
            drawnCardIds.add(card.getId());
            
            // Break early if we've seen both cards
            if (drawnCardIds.size() == 2) {
                break;
            }
        }
        
        // Check that we've seen both cards
        assertEquals(2, drawnCardIds.size());
        assertTrue(drawnCardIds.contains("7choices"));
        assertTrue(drawnCardIds.contains("badsight"));
    }
}
