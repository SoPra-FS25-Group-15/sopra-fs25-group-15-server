package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardEffectType;
import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;


/**
 * ActionCardEffectDTO - Response DTO for the effect of playing an action card
 */
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

    // Getter
    public Long getCardId() {
        return cardId;
    }

    public String getCardName() {
        return cardName;
    }

    public ActionCardType getCardType() {
        return cardType;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public ActionCardEffectType getEffectType() {
        return effectType;
    }

    public String getEffectValue() {
        return effectValue;
    }

    // Setter
    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public void setCardType(ActionCardType cardType) {
        this.cardType = cardType;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName;
    }

    public void setEffectType(ActionCardEffectType effectType) {
        this.effectType = effectType;
    }

    public void setEffectValue(String effectValue) {
        this.effectValue = effectValue;
    }
}
