package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

/**
 * DTO for transferring action card effect data.
 * Used when a player plays an action card and the effect needs to be communicated to clients.
 */
public class ActionCardEffectDTO {
    
    private String effectType;     // Type of effect (e.g., "continent", "blur")
    private String targetPlayer;   // Token of target player (if applicable)
    
    public String getEffectType() {
        return effectType;
    }
    
    public void setEffectType(String effectType) {
        this.effectType = effectType;
    }
    
    public String getTargetPlayer() {
        return targetPlayer;
    }
    
    public void setTargetPlayer(String targetPlayer) {
        this.targetPlayer = targetPlayer;
    }
}
