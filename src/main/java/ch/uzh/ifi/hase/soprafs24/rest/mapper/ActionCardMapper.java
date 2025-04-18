package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ActionCardMapper {

    ActionCardMapper INSTANCE = Mappers.getMapper(ActionCardMapper.class);

    default ActionCardDTO convertEntityToActionCardDTO(ActionCard actionCard) {
        if (actionCard == null) {
            return null;
        }
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId(actionCard.getId());
        dto.setType(actionCard.getType().toString().toLowerCase());
        dto.setTitle(actionCard.getTitle());
        dto.setDescription(actionCard.getDescription());
        return dto;
    }
}