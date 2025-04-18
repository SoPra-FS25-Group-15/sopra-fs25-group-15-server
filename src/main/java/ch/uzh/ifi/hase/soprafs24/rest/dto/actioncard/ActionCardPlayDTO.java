package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

/**
 * ActionCardPlayDTO - Request DTO for playing an action card
 */
public class ActionCardPlayDTO {
    private String cardId;
    private Long targetPlayerId; // optional for powerup cards

    // Getter
    public String getCardId() {
        return cardId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    // Setter
    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }
}