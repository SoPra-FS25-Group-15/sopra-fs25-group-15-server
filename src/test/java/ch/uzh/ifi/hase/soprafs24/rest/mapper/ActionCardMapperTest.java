package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;

public class ActionCardMapperTest {

    private ActionCardMapper actionCardMapper;

    @BeforeEach
    public void setup() {
        actionCardMapper = new ActionCardMapper();
    }

    @Test
    public void testToDTO_FromString() {
        // Create a simple action card DTO from ID
        ActionCardDTO dto = actionCardMapper.toDTO("7choices");

        // Verify mapping
        assertEquals("7choices", dto.getId());
        // Other properties are not set by this method
    }
    
    @Test
    public void testToDTO_WithNullId() {
        // Test with null ID
        ActionCardDTO dto = actionCardMapper.toDTO(null);
        
        // Should return null when input is null
        assertNull(dto);
    }

    @Test
    public void testToId_FromDTO() {
        // Create an ActionCardDTO
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId("badsight");
        dto.setType("punishment");
        dto.setTitle("Bad Sight");
        dto.setDescription("A player of your choice has their screen blurred for the first 15 seconds of the round.");

        // Extract ID from DTO
        String id = actionCardMapper.toId(dto);

        // Verify ID extraction
        assertEquals("badsight", id);
    }
    
    @Test
    public void testToId_WithNullDTO() {
        // Test with null DTO
        String id = actionCardMapper.toId(null);
        
        // Should return null when input is null
        assertNull(id);
    }
}
