package ch.uzh.ifi.hase.soprafs24.service;


import ch.uzh.ifi.hase.soprafs24.constant.ActionCardEffectType;
import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.ActionCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Action Card Service
 * This class is the "worker" and responsible for all functionality related to action cards
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back to the caller.
 */
@Service
@Transactional
public class ActionCardService {

    private final Logger log = LoggerFactory.getLogger(ActionCardService.class);

    private final ActionCardRepository actionCardRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    @Autowired
    public ActionCardService(@Qualifier("actionCardRepository") ActionCardRepository actionCardRepository,
                             @Qualifier("gameRepository") GameRepository gameRepository,
                             @Qualifier("userRepository") UserRepository userRepository,
                             WebSocketService webSocketService) {
        this.actionCardRepository = actionCardRepository;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
    }

    /**
     * Get all action cards for a player in a game
     *
     * @param gameId - the game ID
     * @param userId - the user ID
     * @return List of ActionCard objects
     */
    public List<ActionCard> getPlayerActionCards(Long gameId, Long userId) {
        // Validate game and user
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the user is in the game
        if (!game.getPlayers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a player in this game");
        }

        // Return the action cards
        return actionCardRepository.findByOwnerAndGame(user, game);
    }

    /**
     * Draw a new action card for a player
     *
     * @param gameId - the game ID
     * @param userId - the user ID
     * @return the drawn ActionCard
     */
    public ActionCard drawActionCard(Long gameId, Long userId) {
        // Validate game and user
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the user is in the game
        if (!game.getPlayers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a player in this game");
        }

        // Check if the user has reached the maximum number of action cards (5)
        List<ActionCard> userCards = actionCardRepository.findByOwnerAndGame(user, game);
        if (userCards.size() >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Player already has the maximum number of action cards (5). Discard one first.");
        }

        // Create a new random action card
        ActionCard newCard = createRandomActionCard();
        newCard.setOwner(user);
        newCard.setGame(game);
        newCard.setActive(false);

        // Save and return the new card
        ActionCard savedCard = actionCardRepository.save(newCard);

        // Notify all players that a card has been drawn
        webSocketService.sendToGame(gameId, "/topic/game/" + gameId + "/cards",
                "Player " + user.getId() + " has drawn a new action card.");

        return savedCard;
    }

    /**
     * Play an action card
     *
     * @param gameId - the game ID
     * @param userId - the user ID
     * @param actionCardPlayDTO - contains the action card ID and target player ID (if applicable)
     * @return ActionCardEffectDTO with the effect details
     */
    public ActionCardEffectDTO playActionCard(Long gameId, Long userId, ActionCardPlayDTO actionCardPlayDTO) {
        // Validate game and user
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the user is in the game
        if (!game.getPlayers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a player in this game");
        }

        // Find the action card
        ActionCard actionCard = actionCardRepository.findById(actionCardPlayDTO.getActionCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action card not found"));

        // Check if the user owns the card
        if (!actionCard.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not own this action card");
        }

        // For punishment cards, check if target player is specified and valid
        User targetPlayer = null;
        if (actionCard.getType() == ActionCardType.PUNISHMENT) {
            if (actionCardPlayDTO.getTargetPlayerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Target player ID is required for punishment cards");
            }

            targetPlayer = userRepository.findById(actionCardPlayDTO.getTargetPlayerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target player not found"));

            if (!game.getPlayers().contains(targetPlayer)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Target player is not in this game");
            }

            // Cannot target self
            if (targetPlayer.getId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot target yourself with a punishment card");
            }
        }

        // Apply the card effect and get the result
        ActionCardEffectDTO effectDTO = applyCardEffect(actionCard, game, user, targetPlayer);

        // Mark the card as active if needed, or remove it if it's a one-time use
        actionCard.setActive(true);
        actionCardRepository.save(actionCard);

        // Notify all players about the played card
        webSocketService.sendToGame(gameId, "/topic/game/" + gameId + "/cards/played", effectDTO);

        return effectDTO;
    }

    /**
     * Get all active action cards in a game
     *
     * @param gameId - the game ID
     * @return List of active ActionCard objects
     */
    public List<ActionCard> getActiveActionCards(Long gameId) {
        // Validate game
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // Return the active action cards
        return actionCardRepository.findByGameAndIsActiveTrue(game);
    }

    /**
     * Discard an action card
     *
     * @param gameId - the game ID
     * @param userId - the user ID
     * @param actionCardId - the action card ID to discard
     */
    public void discardActionCard(Long gameId, Long userId, Long actionCardId) {
        // Validate game and user
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check if the user is in the game
        if (!game.getPlayers().contains(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a player in this game");
        }

        // Find the action card
        ActionCard actionCard = actionCardRepository.findById(actionCardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action card not found"));

        // Check if the user owns the card
        if (!actionCard.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not own this action card");
        }

        // Delete the card
        actionCardRepository.delete(actionCard);

        // Notify all players that a card has been discarded
        webSocketService.sendToGame(gameId, "/topic/game/" + gameId + "/cards",
                "Player " + user.getId() + " has discarded an action card.");
    }

    /**
     * Create a random action card
     *
     * @return a new random ActionCard object
     */
    private ActionCard createRandomActionCard() {
        ActionCard card = new ActionCard();
        Random random = new Random();

        // Determine card type (50% chance for each type)
        if (random.nextBoolean()) {
            card.setType(ActionCardType.POWERUP);
            createRandomPowerupCard(card, random);
        } else {
            card.setType(ActionCardType.PUNISHMENT);
            createRandomPunishmentCard(card, random);
        }

        return card;
    }

    /**
     * Create a random powerup card
     *
     * @param card - the ActionCard object to configure
     * @param random - Random instance for selection
     */
    private void createRandomPowerupCard(ActionCard card, Random random) {
        // Define all powerup cards
        List<Map<String, Object>> powerupCards = new ArrayList<>();

        powerupCards.add(Map.of(
                "name", "7 Choices",
                "effect", "Reveal the continent of the target location.",
                "effectType", ActionCardEffectType.CONTINENT_REVEAL
        ));

        powerupCards.add(Map.of(
                "name", "High hopes",
                "effect", "Reveal the height in meters above sea level of the location.",
                "effectType", ActionCardEffectType.HEIGHT_REVEAL
        ));

        powerupCards.add(Map.of(
                "name", "Temperature",
                "effect", "Reveal the average winter and summer temperature at the location.",
                "effectType", ActionCardEffectType.TEMPERATURE_REVEAL
        ));

        powerupCards.add(Map.of(
                "name", "Draw again",
                "effect", "Discard 2 powerup cards (this card included) and draw a powerup card of your choice.",
                "effectType", ActionCardEffectType.DRAW_AGAIN
        ));

        powerupCards.add(Map.of(
                "name", "Swap",
                "effect", "Switch this card with the powerup card of another player.",
                "effectType", ActionCardEffectType.SWAP_CARD
        ));

        powerupCards.add(Map.of(
                "name", "Clear Vision",
                "effect", "Keep your screen unblurred for the whole round.",
                "effectType", ActionCardEffectType.CLEAR_VISION
        ));

        powerupCards.add(Map.of(
                "name", "Cheat Code",
                "effect", "Your distance from the target location is halved for scoring.",
                "effectType", ActionCardEffectType.HALF_DISTANCE
        ));

        powerupCards.add(Map.of(
                "name", "One More",
                "effect", "Have an additional guess this round.",
                "effectType", ActionCardEffectType.ADDITIONAL_GUESS
        ));

        powerupCards.add(Map.of(
                "name", "Time is ticking",
                "effect", "Add 15 seconds to your timer.",
                "effectType", ActionCardEffectType.ADD_TIME
        ));

        powerupCards.add(Map.of(
                "name", "Study time",
                "effect", "Reveal the distance from the UZH Building to your target location.",
                "effectType", ActionCardEffectType.REVEAL_UZH_DISTANCE
        ));

        // Select a random powerup card
        Map<String, Object> selectedCard = powerupCards.get(random.nextInt(powerupCards.size()));

        // Set card properties
        card.setName((String) selectedCard.get("name"));
        card.setEffect((String) selectedCard.get("effect"));
    }

    /**
     * Create a random punishment card
     *
     * @param card - the ActionCard object to configure
     * @param random - Random instance for selection
     */
    private void createRandomPunishmentCard(ActionCard card, Random random) {
        // Define all punishment cards
        List<Map<String, Object>> punishmentCards = new ArrayList<>();

        punishmentCards.add(Map.of(
                "name", "+1",
                "effect", "A player of your choice needs to pick up one round card at the beginning of the next round.",
                "effectType", ActionCardEffectType.ADD_ROUND_CARD
        ));

        punishmentCards.add(Map.of(
                "name", "No Action",
                "effect", "A player of your choice doesn't receive an action card at the beginning of the next round.",
                "effectType", ActionCardEffectType.NO_ACTION_CARD
        ));

        punishmentCards.add(Map.of(
                "name", "No Labels",
                "effect", "A player of your choice plays this round with a map that has no labels for countries, cities and streets.",
                "effectType", ActionCardEffectType.NO_LABELS
        ));

        punishmentCards.add(Map.of(
                "name", "Trashcan",
                "effect", "A player of your choice has to discard an action card of their choice, if they have any.",
                "effectType", ActionCardEffectType.DISCARD_CARD
        ));

        punishmentCards.add(Map.of(
                "name", "Bad sight",
                "effect", "A player of your choice has their screen blurred for the first 15 seconds of the round.",
                "effectType", ActionCardEffectType.BLUR_SCREEN
        ));

        punishmentCards.add(Map.of(
                "name", "Rooted",
                "effect", "A player of your choice can't move around for the first 15 seconds of the round.",
                "effectType", ActionCardEffectType.NO_MOVEMENT
        ));

        punishmentCards.add(Map.of(
                "name", "Bad guess",
                "effect", "A player of your choice has their distance from the target location doubled for scoring.",
                "effectType", ActionCardEffectType.DOUBLE_DISTANCE
        ));

        punishmentCards.add(Map.of(
                "name", "Restricted",
                "effect", "A player of your choice has a maximum of 1 guess this round under any circumstance.",
                "effectType", ActionCardEffectType.RESTRICT_GUESS
        ));

        punishmentCards.add(Map.of(
                "name", "Time runs out",
                "effect", "A player of your choice has 15 seconds removed from their timer.",
                "effectType", ActionCardEffectType.REDUCE_TIME
        ));

        punishmentCards.add(Map.of(
                "name", "No help",
                "effect", "A player of your choice cannot play any action cards this round.",
                "effectType", ActionCardEffectType.BLOCK_CARDS
        ));

        // Select a random punishment card
        Map<String, Object> selectedCard = punishmentCards.get(random.nextInt(punishmentCards.size()));

        // Set card properties
        card.setName((String) selectedCard.get("name"));
        card.setEffect((String) selectedCard.get("effect"));
    }

    /**
     * Apply the effect of an action card
     *
     * @param actionCard - the ActionCard to apply
     * @param game - the Game
     * @param player - the Player using the card
     * @param targetPlayer - the target Player (null for powerup cards)
     * @return ActionCardEffectDTO with the effect details
     */
    private ActionCardEffectDTO applyCardEffect(ActionCard actionCard, Game game, User player, User targetPlayer) {
        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
        effectDTO.setCardId(actionCard.getId());
        effectDTO.setCardName(actionCard.getName());
        effectDTO.setCardType(actionCard.getType());
        effectDTO.setPlayerId(player.getId());

        if (targetPlayer != null) {
            effectDTO.setTargetPlayerId(targetPlayer.getId());
        }

        // Apply specific effect based on card name
        switch (actionCard.getName()) {
            // Powerup cards
            case "7 Choices":
                // Simulate revealing continent (in real implementation, get from game state)
                String[] continents = {"Asia", "Africa", "North America", "South America", "Antarctica", "Europe", "Australia"};
                String continent = continents[new Random().nextInt(continents.length)];
                effectDTO.setEffectType(ActionCardEffectType.CONTINENT_REVEAL);
                effectDTO.setEffectValue(continent);
                break;

            case "High hopes":
                // Simulate revealing height (in real implementation, get from game state)
                int height = new Random().nextInt(2000);
                effectDTO.setEffectType(ActionCardEffectType.HEIGHT_REVEAL);
                effectDTO.setEffectValue(String.valueOf(height) + " meters");
                break;

            case "Temperature":
                // Simulate revealing temperature (in real implementation, get from game state)
                int winterTemp = new Random().nextInt(30) - 15;
                int summerTemp = new Random().nextInt(20) + 15;
                effectDTO.setEffectType(ActionCardEffectType.TEMPERATURE_REVEAL);
                effectDTO.setEffectValue("Winter: " + winterTemp + "°C, Summer: " + summerTemp + "°C");
                break;

            // Other powerup cards implementation...

            // Punishment cards
            case "No Labels":
                effectDTO.setEffectType(ActionCardEffectType.NO_LABELS);
                effectDTO.setEffectValue("Map labels are hidden for " + targetPlayer.getId());
                break;

            case "Bad sight":
                effectDTO.setEffectType(ActionCardEffectType.BLUR_SCREEN);
                effectDTO.setEffectValue("Screen blurred for " + targetPlayer.getId() + " for 15 seconds");
                break;

            // Other punishment cards implementation...

            default:
                effectDTO.setEffectType(null);
                effectDTO.setEffectValue("Card effect not implemented yet");
        }

        return effectDTO;
    }
}