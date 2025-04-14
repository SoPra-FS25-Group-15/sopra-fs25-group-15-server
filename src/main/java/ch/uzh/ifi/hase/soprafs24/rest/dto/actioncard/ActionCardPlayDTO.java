package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

/**
 * ActionCardPlayDTO - Request DTO for playing an action card
 */
public class ActionCardPlayDTO {
    private Long actionCardId;
    private Long targetPlayerId;

    // Getter
    public Long getActionCardId() {
        return actionCardId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    // Setter
    public void setActionCardId(Long actionCardId) {
        this.actionCardId = actionCardId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}