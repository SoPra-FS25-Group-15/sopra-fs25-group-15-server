package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ActionCardMapper {

    ActionCardMapper INSTANCE = Mappers.getMapper(ActionCardMapper.class);

    // Here is the newly added action card conversion method
    @Mapping(source = "owner.id", target = "ownerId")
    ActionCardDTO convertEntityToActionCardDTO(ActionCard actionCard);
}