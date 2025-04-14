package ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard;


import java.util.List;

/**
 * ActionCardGetDTO - Response DTO for getting action cards
 */
public class ActionCardGetDTO {
    private List<ActionCardDTO> actionCards;
    public List<ActionCardDTO> getActionCards() {
        return actionCards;}
   public void setActionCards(List<ActionCardDTO> actionCards) {
        this.actionCards = actionCards;}
}
