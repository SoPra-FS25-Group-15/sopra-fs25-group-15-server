package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionCardDTOTest {

    @Test
    void testActionCardDTO() {
        // Create a DTO
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId("7choices");
        dto.setType("powerup");
        dto.setTitle("7 Choices");
        dto.setDescription("Reveal the continent of the target location.");

        // Verify getters
        assertEquals("7choices", dto.getId());
        assertEquals("powerup", dto.getType());
        assertEquals("7 Choices", dto.getTitle());
        assertEquals("Reveal the continent of the target location.", dto.getDescription());
    }

    @Test
    void testActionCardPlayDTO() {
        // Create a DTO
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        dto.setCardId("badsight");
        dto.setTargetPlayerId(123L);

        // Verify getters
        assertEquals("badsight", dto.getCardId());
        assertEquals(123L, dto.getTargetPlayerId());
    }
}
