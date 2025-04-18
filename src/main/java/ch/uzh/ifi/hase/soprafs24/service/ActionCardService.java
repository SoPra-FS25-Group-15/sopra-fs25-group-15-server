package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * ActionCardService – Provides hard‑coded action cards and play logic.
 */
@Service
public class ActionCardService {

    private static final List<ActionCardDTO> CARDS = List.of(
        create("7choices", "powerup",    "7 Choices",
               "Reveal the continent of the target location."),
        create("badsight", "punishment", "Bad Sight",
               "A player of your choice has their screen blurred for the first 15 seconds of the round.")
    );

    private final Random random = new Random();

    /**
     * Draws one of the two hard‑coded cards at random.
     */
    public ActionCardDTO drawRandomCard() {
        return CARDS.get(random.nextInt(CARDS.size()));
    }

    private static ActionCardDTO create(String id, String type, String title, String desc) {
        ActionCardDTO dto = new ActionCardDTO();
        dto.setId(id);
        dto.setType(type);
        dto.setTitle(title);
        dto.setDescription(desc);
        return dto;
    }
}