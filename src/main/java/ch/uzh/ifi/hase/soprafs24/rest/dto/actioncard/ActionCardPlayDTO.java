package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import lombok.Data;

/**
 * ActionCardPlayDTO - Request DTO for playing an action card
 */
@Data
public class ActionCardPlayDTO {
    private Long actionCardId;
    private Long targetPlayerId;

}