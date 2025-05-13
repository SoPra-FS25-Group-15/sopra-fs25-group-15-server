package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActionCardPlayDTOTest {

    @Test
    void testDefaultConstructorAndInitialState() {
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        assertNull(dto.getActionCardId(), "actionCardId should be null after default construction.");
    }

    @Test
    void testSetAndGetActionCardId() {
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        String cardId = "skipTurnCard123";
        dto.setActionCardId(cardId);
        assertEquals(cardId, dto.getActionCardId(), "getActionCardId should return the value set by setActionCardId.");
    }

    @Test
    void testSetActionCardIdWithNull() {
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        dto.setActionCardId(null);
        assertNull(dto.getActionCardId(), "getActionCardId should return null if null was set.");
    }

    @Test
    void testSetActionCardIdWithEmptyString() {
        ActionCardPlayDTO dto = new ActionCardPlayDTO();
        String cardId = "";
        dto.setActionCardId(cardId);
        assertEquals(cardId, dto.getActionCardId(), "getActionCardId should return an empty string if an empty string was set.");
    }
}