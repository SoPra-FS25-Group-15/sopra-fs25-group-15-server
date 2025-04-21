package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper class to convert between ActionCard entities and DTOs
 */
@Component
public class ActionCardMapper {

    /**
     * Convert ActionCardDTO to a string ID representation
     * @param dto ActionCardDTO to convert
     * @return String ID of the action card
     */
    public String toId(ActionCardDTO dto) {
        if (dto == null) {
            return null;
        }
        return dto.getId();
    }
    
    /**
     * Create a simple ActionCardDTO with only an ID
     * @param id ID of the action card
     * @return ActionCardDTO with the given ID
     */
    public ActionCardDTO toDTO(String id) {
        if (id == null) {
            return null;
        }
        
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId(id);
        return dto;
    }
}