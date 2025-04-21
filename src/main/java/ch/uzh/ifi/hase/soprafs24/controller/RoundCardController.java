package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.RoundCard;
import ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Round Card Controller
 * This class is responsible for handling all REST request that are related to round cards.
 * The controller will receive the request and delegate the execution to the RoundCardService and finally return the result.
 */
@RestController
public class RoundCardController {

    private final RoundCardService roundCardService;

    RoundCardController(RoundCardService roundCardService) {
        this.roundCardService = roundCardService;
    }

    /**
     * GET /roundcards : Get all round cards
     *
     * @return the list of all round cards
     */
    @GetMapping("/roundcards")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<RoundCardDTO> getAllRoundCards() {
        // Fetch all round cards from the service
        List<RoundCard> roundCards = roundCardService.getAllRoundCards();

        // Convert them to DTOs
        List<RoundCardDTO> roundCardDTOs = new ArrayList<>();
        for (RoundCard roundCard : roundCards) {
            roundCardDTOs.add(ActionCardMapper.INSTANCE.convertEntityToRoundCardDTO(roundCard));
        }

        return roundCardDTOs;
    }

    /**
     * GET /players/{playerId}/roundcards : Get round cards for a specific player
     *
     * @param playerId the ID of the player
     * @return the list of round cards available for the player
     */
    @GetMapping("/players/{playerId}/roundcards")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<RoundCardDTO> getPlayerRoundCards(@PathVariable Long playerId) {
        // Fetch round cards for the specific player
        List<RoundCard> roundCards = roundCardService.getPlayerRoundCards(playerId);

        // Convert them to DTOs
        List<RoundCardDTO> roundCardDTOs = new ArrayList<>();
        for (RoundCard roundCard : roundCards) {
            roundCardDTOs.add(ActionCardMapper.INSTANCE.convertEntityToRoundCardDTO(roundCard));
        }

        return roundCardDTOs;
    }
}