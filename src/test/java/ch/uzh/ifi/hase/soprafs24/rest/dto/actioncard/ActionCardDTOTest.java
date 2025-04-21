package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class ActionCardDTOTest {
    
    private ActionCardDTO powerupCard;
    private ActionCardDTO punishmentCard;

    @BeforeEach
    void setup() {
        // Setup a powerup card that provides information
        powerupCard = new ActionCardDTO();
        powerupCard.setId("7choices");
        powerupCard.setType("powerup");
        powerupCard.setTitle("7 Choices");
        powerupCard.setDescription("Reveal the continent of the target location.");

        // Setup a punishment card that hinders other players
        punishmentCard = new ActionCardDTO();
        punishmentCard.setId("badsight");
        punishmentCard.setType("punishment");  // Changed from "debuff" to "punishment" for consistency
        punishmentCard.setTitle("Bad Sight");
        punishmentCard.setDescription("Apply blur effect to target player's screen.");
    }

    @Test
    void testActionCardDTO_PowerUp() {
        // Verify powerup card properties
        assertEquals("7choices", powerupCard.getId());
        assertEquals("powerup", powerupCard.getType());
        assertEquals("7 Choices", powerupCard.getTitle());
        assertEquals("Reveal the continent of the target location.", powerupCard.getDescription());
        
        // Test that card types are correctly differentiated
        assertTrue(powerupCard.getType().equals("powerup"));
        assertFalse(powerupCard.getType().equals("punishment"));
    }
    
    @Test
    void testActionCardDTO_Punishment() {
        // Verify punishment card properties
        assertEquals("badsight", punishmentCard.getId());
        assertEquals("punishment", punishmentCard.getType());  // Changed from "debuff" to "punishment"
        assertEquals("Bad Sight", punishmentCard.getTitle());
        assertEquals("Apply blur effect to target player's screen.", punishmentCard.getDescription());
        
        // Test that card types are correctly differentiated
        assertTrue(punishmentCard.getType().equals("punishment"));  // Changed from "debuff" to "punishment"
        assertFalse(punishmentCard.getType().equals("powerup"));
    }

    @Test
    void testActionCardPlayDTO_WithTarget() {
        // Create a DTO for playing a card against another player
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        dto.setCardId("badsight");
        dto.setTargetPlayerId(123L);

        // Verify getters
        assertEquals("badsight", dto.getCardId());
        assertEquals(123L, dto.getTargetPlayerId());
    }
    
    @Test
    void testActionCardPlayDTO_WithoutTarget() {
        // Create a DTO for playing a self-buff card with no target
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        dto.setCardId("7choices");
        // No target player specified for information cards
        
        // Verify getters
        assertEquals("7choices", dto.getCardId());
        assertNull(dto.getTargetPlayerId());
    }
    
    @Test
    void testActionCardPlayDTO_ModifyingProperties() {
        // Test modifying properties of an existing DTO
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        dto.setCardId("initialCard");
        dto.setTargetPlayerId(100L);
        
        // Modify properties
        dto.setCardId("newCard");
        dto.setTargetPlayerId(200L);
        
        // Verify updated values
        assertEquals("newCard", dto.getCardId());
        assertEquals(200L, dto.getTargetPlayerId());
    }
    
    @Test
    void testActionCardEffectDTO() {
        // Create an effect DTO to test effect data transfer
        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
        effectDTO.setEffectType("blur");
        effectDTO.setTargetPlayer("player2");
        
        // Verify getters
        assertEquals("blur", effectDTO.getEffectType());
        assertEquals("player2", effectDTO.getTargetPlayer());
    }
}
