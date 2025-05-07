package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.Optional;

/**
 * ActionCardService – Provides hard‑coded action cards and play logic.
 */
@Service
public class ActionCardService {

    private static final Logger log = LoggerFactory.getLogger(ActionCardService.class);

    private static final List<ActionCardDTO> CARDS = List.of(
            create("7choices", "powerup",    "7 Choices",
                    "Reveal the continent of the target location."),
            create("badsight", "punishment", "Bad Sight",
                    "A player of your choice has their screen blurred for the first 15 seconds of the round."),
            create("clearvision", "powerup", "Clear Vision",
                    "Keep your screen unblurred for the whole round."),
            create("nolabels", "punishment", "No Labels",
                    "A player of your choice plays this round with a map that has no labels for countries, cities and streets.")
    );

    private final Random random = new Random();
    
    @Autowired
    private GameService gameService;

    /**
     * Draws one of the two hard‑coded cards at random.
     */
    public ActionCardDTO drawRandomCard() {
        return CARDS.get(random.nextInt(CARDS.size()));
    }

    /**
     * Find a card by its ID
     */
    public ActionCardDTO findById(String cardId) {
        log.debug("Looking for card with ID: {}", cardId);
        Optional<ActionCardDTO> card = CARDS.stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst();
        
        if (card.isEmpty()) {
            log.warn("Card with ID {} not found", cardId);
        }
        
        return card.orElse(null);
    }

    /**
     * Validates if the specified action card ID exists
     */
    public boolean isValidActionCard(String cardId) {
        return CARDS.stream().anyMatch(card -> card.getId().equals(cardId));
    }

    /**
     * Processes the effect of an action card
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ActionCardEffectDTO processActionCardEffect(String cardId, String targetPlayerToken) {
        log.info("Processing action card effect for card: {}, target player: {}", cardId, targetPlayerToken);

        ActionCardDTO card = findById(cardId);
        if (card == null) {
            log.error("Cannot process effect - card not found: {}", cardId);
            return null;
        }

        ActionCardEffectDTO effect = new ActionCardEffectDTO();

        switch(cardId) {
            case "7choices":
                effect.setEffectType("continent");
                break;
            case "badsight":
                effect.setEffectType("blur");
                effect.setTargetPlayer(targetPlayerToken);
                break;
            case "clearvision":
                effect.setEffectType("unblur");
                break;
            case "nolabels":
                effect.setEffectType("nolabels");
                effect.setTargetPlayer(targetPlayerToken);
                break;
            default:
                log.warn("Unknown action card effect for ID: {}", cardId);
                effect.setEffectType("unknown");
        }

        log.info("Processed action card effect: {}", effect.getEffectType());
        return effect;
    }
    
    /**
     * Process action card for a specific game - ensuring transaction boundaries
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ActionCardEffectDTO processActionCardForGame(Long gameId, String playerToken, String cardId, String targetPlayerToken) {
        ActionCardEffectDTO effect = processActionCardEffect(cardId, targetPlayerToken);

        if (effect != null) {
            // Apply action card to target player if specified
            if (targetPlayerToken != null && ("blur".equals(effect.getEffectType()) || "nolabels".equals(effect.getEffectType()))) {
                gameService.applyActionCardToPlayer(gameId, targetPlayerToken, cardId);
            }

            // Apply to self if no target or it's a self-buff
            if (targetPlayerToken == null || "continent".equals(effect.getEffectType()) || "unblur".equals(effect.getEffectType())) {
                gameService.applyActionCardToPlayer(gameId, playerToken, cardId);
            }
        }

        return effect;
    }

    private static ActionCardDTO create(String id, String type, String title, String desc) {
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId(id);
        dto.setType(type);
        dto.setTitle(title);
        dto.setDescription(desc);
        return dto;
    }
    
    /**
     * Get continent name from coordinates 
     */
    public String getContinent(double latitude, double longitude) {
        if (latitude > 34 && longitude > -10 && longitude < 40) {
            return "Europe";
        }
        else if (latitude > 0 && longitude > 40 && !(latitude < 10 && longitude > 110)) {
            return "Asia";
        }
        else if (latitude < 0 && latitude > -35 && longitude > -20 && longitude < 55) {
            return "Africa";
        }
        else if (latitude > 0 && longitude < -30 && longitude > -170) {
            return "North America";
        }
        else if (latitude < 0 && longitude < -30 && longitude > -80) {
            return "South America";
        }
        else if (latitude < -10 && longitude > 110 && longitude < 180) {
            return "Australia";
        }
        else if (latitude < -60) {
            return "Antarctica";
        }
        else {
            return "Unknown";
        }
    }
}