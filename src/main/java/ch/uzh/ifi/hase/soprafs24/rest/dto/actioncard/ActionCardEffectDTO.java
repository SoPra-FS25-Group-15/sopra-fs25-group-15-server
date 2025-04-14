package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardEffectType;
import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import lombok.Data;

/**
 * ActionCardEffectDTO - Response DTO for the effect of playing an action card
 */
@Data
public class ActionCardEffectDTO {
    private Long cardId;
    private String cardName;
    private ActionCardType cardType;
    private Long playerId;
    private String playerName;
    private Long targetPlayerId;
    private String targetPlayerName;
    private ActionCardEffectType effectType;
    private String effectValue;

}
