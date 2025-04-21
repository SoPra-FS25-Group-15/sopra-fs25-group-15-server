package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;

import java.util.List;
import java.util.Map;

public class GameDataResponseDTO {
    private List<RoundCardDTO> roundCards;
    private Map<Long, List<ActionCardDTO>> actionCards; // userId -> list of action cards
    
    public List<RoundCardDTO> getRoundCards() {
        return roundCards;
    }
    
    public void setRoundCards(List<RoundCardDTO> roundCards) {
        this.roundCards = roundCards;
    }
    
    public Map<Long, List<ActionCardDTO>> getActionCards() {
        return actionCards;
    }
    
    public void setActionCards(Map<Long, List<ActionCardDTO>> actionCards) {
        this.actionCards = actionCards;
    }
}
