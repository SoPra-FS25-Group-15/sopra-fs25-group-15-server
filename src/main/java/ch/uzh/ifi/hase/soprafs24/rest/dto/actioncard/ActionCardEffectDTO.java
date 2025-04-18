package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;

/**
 * DTO for Action Card Effect
 * This class is used to transfer action card effect data between frontend and backend
 */
public class ActionCardEffectDTO {
    
    private String effectType;
    private String targetPlayer;
    // Add other fields that might be needed for your action card effects
    
    // Getters and Setters
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
