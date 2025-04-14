package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import lombok.Data;

import java.util.List;

/**
 * ActionCardGetDTO - Response DTO for getting action cards
 */
@Data
public class ActionCardGetDTO {
    private List<ActionCardDTO> actionCards;

}
