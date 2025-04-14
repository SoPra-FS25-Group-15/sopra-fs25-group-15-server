package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import lombok.Data;

@Data
public class ActionCardDTO {
    private Long id;
    private String name;
    private ActionCardType type;
    private String effect;
    private boolean isActive;
    private Long ownerId;


}