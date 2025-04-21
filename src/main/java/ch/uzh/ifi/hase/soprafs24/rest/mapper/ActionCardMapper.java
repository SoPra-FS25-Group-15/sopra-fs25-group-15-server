package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.RoundCard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard.RoundCardDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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


    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "guessType", target = "guessType")
    @Mapping(source = "streetViewType", target = "streetViewType")
    @Mapping(source = "roundTimeInSeconds", target = "roundTimeInSeconds")
    @Mapping(source = "mapType", target = "mapType")
    RoundCardDTO convertEntityToRoundCardDTO(RoundCard roundCard);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "guessType", target = "guessType")
    @Mapping(source = "streetViewType", target = "streetViewType")
    @Mapping(source = "roundTimeInSeconds", target = "roundTimeInSeconds")
    @Mapping(source = "mapType", target = "mapType")
    RoundCard convertRoundCardDTOtoEntity(RoundCardDTO roundCardDTO);
}