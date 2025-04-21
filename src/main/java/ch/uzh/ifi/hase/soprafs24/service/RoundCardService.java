package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.RoundCardType;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.RoundCard;
import ch.uzh.ifi.hase.soprafs24.entity.PlayerRoundCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.RoundCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRoundCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class RoundCardService {

    private final Logger log = LoggerFactory.getLogger(RoundCardService.class);

    private final RoundCardRepository roundCardRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final PlayerRoundCardRepository playerRoundCardRepository;

    @Autowired
    public RoundCardService(@Qualifier("roundCardRepository") RoundCardRepository roundCardRepository,
                            @Qualifier("userRepository") UserRepository userRepository,
                            @Qualifier("gameRepository") GameRepository gameRepository,
                            @Qualifier("playerRoundCardRepository") PlayerRoundCardRepository playerRoundCardRepository) {
        this.roundCardRepository = roundCardRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.playerRoundCardRepository = playerRoundCardRepository;
    }

    /**
     * Initialize the database with predefined round cards if empty
     */
    @PostConstruct
    private void initializeRoundCards() {
        // Check if round cards already exist
        if (roundCardRepository.count() > 0) {
            return;
        }

        // Create all round cards according to the game rules
        createRoundCard("World", "The round includes the full available coverage", RoundCardType.WORLD,
                "Precise", "Standard", 60, "Standard");

        createRoundCard("Flash", "The round includes the full available coverage, but the round time is halved",
                RoundCardType.FLASH, "Precise", "Standard", 30, "Standard");

        createRoundCard("Radio", "The round includes the full available coverage of countries, but instead of a street view image, a local radio station is played",
                RoundCardType.RADIO, "Precise", "Not shown", 60, "Standard");

        createRoundCard("No Move", "The round includes the full available coverage, but the player can't move, looking around and zooming is still possible",
                RoundCardType.NO_MOVE, "Precise", "No Move", 60, "Standard");

        createRoundCard("No Move, Pan, Zoom", "The round includes the full available coverage, but the player can't move, can't look around and can't zoom in",
                RoundCardType.NO_MOVE_PAN_ZOOM, "Precise", "NMPZ", 60, "Standard");

        createRoundCard("Hangover", "The round includes the full available coverage, but the street view image is slightly blurred/distorted",
                RoundCardType.HANGOVER, "Precise", "Slightly blurred", 60, "Standard");

        createRoundCard("Lost in Transmission", "The round includes the full available coverage, but the map to lock in your guess has no labels for countries, cities and streets",
                RoundCardType.LOST_IN_TRANSMISSION, "Precise", "Standard", 60, "No Labels");

        createRoundCard("Double", "The round includes the full available coverage, but the player has two guesses. The better one counts.",
                RoundCardType.DOUBLE, "2x Precise", "Standard", 60, "Standard");
    }

    /**
     * Helper method to create a round card in the database
     */
    private void createRoundCard(String name, String description, RoundCardType type,
                                 String guessType, String streetViewType,
                                 Integer roundTimeInSeconds, String mapType) {
        RoundCard roundCard = new RoundCard();
        roundCard.setName(name);
        roundCard.setDescription(description);
        roundCard.setType(type);
        roundCard.setGuessType(guessType);
        roundCard.setStreetViewType(streetViewType);
        roundCard.setRoundTimeInSeconds(roundTimeInSeconds);
        roundCard.setMapType(mapType);

        roundCardRepository.save(roundCard);
        roundCardRepository.flush();
    }

    /**
     * Get all round cards
     */
    public List<RoundCard> getAllRoundCards() {
        return this.roundCardRepository.findAll();
    }

    /**
     * Get available round cards for a player
     */
    public List<RoundCard> getPlayerRoundCards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Get the player's unused round cards
        List<PlayerRoundCard> playerRoundCards = playerRoundCardRepository.findByUserAndUsedFalse(user);

        // Convert PlayerRoundCard to RoundCard
        List<RoundCard> roundCards = new ArrayList<>();
        for (PlayerRoundCard playerRoundCard : playerRoundCards) {
            roundCards.add(playerRoundCard.getRoundCard());
        }

        return roundCards;
    }

    /**
     * Submit a round card for a player in a game
     */
    public Game submitRoundCard(Long userId, Long roundCardId, Long gameId) {
        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Get game
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // Check if it's this player's turn
        if (!game.getCurrentPlayerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your turn");
        }

        // Find the player's round card
        List<PlayerRoundCard> playerCards = playerRoundCardRepository.findByUserAndUsedFalse(user);
        PlayerRoundCard selectedCard = null;

        for (PlayerRoundCard card : playerCards) {
            if (card.getRoundCard().getId().equals(roundCardId)) {
                selectedCard = card;
                break;
            }
        }

        if (selectedCard == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Round card not found or already used");
        }

        // Update game with the selected round card
        game.setCurrentRoundCard(selectedCard.getRoundCard());

        // Mark the card as used
        selectedCard.setUsed(true);
        playerRoundCardRepository.save(selectedCard);

        // Save updated game
        return gameRepository.save(game);
    }

    /**
     * Assign initial round cards to a player
     */
    public void assignInitialRoundCards(User user, int cardCount) {
        // Get the World card type only
        RoundCard worldCard = null;
        List<RoundCard> allCards = getAllRoundCards();
        for (RoundCard card : allCards) {
            if (card.getType() == RoundCardType.WORLD) {
                worldCard = card;
                break;
            }
        }

        if (worldCard == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "World card type not found");
        }

        // Create the specified number of player cards - all World cards
        for (int i = 0; i < cardCount; i++) {
            PlayerRoundCard playerCard = new PlayerRoundCard();
            playerCard.setUser(user);
            playerCard.setRoundCard(worldCard);
            playerCard.setUsed(false);

            playerRoundCardRepository.save(playerCard);
        }

        log.info("Assigned {} World round cards to user {}", cardCount, user.getId());
    }
}