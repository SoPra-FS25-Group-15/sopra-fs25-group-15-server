package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing round cards in the game
 */
@Service
@Transactional
public class RoundCardService {
    private final Logger log = LoggerFactory.getLogger(RoundCardService.class);

    // ID‑based storage: gameId → playerId → their cards
    private final Map<Long, Map<Long, List<RoundCardDTO>>> playerRoundCards = new ConcurrentHashMap<>();

    // Token‑based storage: gameId → token → their cards
    private final Map<Long, Map<String, List<RoundCardDTO>>> tokenRoundCards = new ConcurrentHashMap<>();

    private final Random random = new Random();

    // The two card types we support
    private static final List<String> ROUND_CARDS = List.of("world", "flash");

    @Autowired
    private AuthService authService;

    @Autowired @Lazy
    private GameService gameService;

    private RoundCardDTO createRoundCard(String id, String name, String description, int timeSeconds) {
        RoundCardDTO card = new RoundCardDTO();
        card.setId(id);
        card.setName(name);
        card.setDescription(description);

        RoundCardModifiers modifiers = new RoundCardModifiers();
        modifiers.setGuessType("Precise");
        modifiers.setStreetView("Standard");
        modifiers.setTime(timeSeconds);

        card.setModifiers(modifiers);
        return card;
    }

    /**
     * Assign initial (ID‑based) round cards to each player.
     */
    public void assignInitialRoundCards(Long gameId, List<Long> playerIds) {
        Map<Long, List<RoundCardDTO>> gameCards = new HashMap<>();
        for (Long playerId : playerIds) {
            List<RoundCardDTO> cards = new ArrayList<>();
            cards.add(createRoundCard(
                "world-" + playerId + "-" + System.currentTimeMillis(),
                "World",
                "The round includes the full available coverage",
                60
            ));
            cards.add(createRoundCard(
                "flash-" + playerId + "-" + System.currentTimeMillis(),
                "Flash",
                "The round includes the full available coverage, but the round time is halved",
                30
            ));
            gameCards.put(playerId, cards);
            log.info("Assigned {} ID‑based cards to player {} in game {}",
                     cards.size(), playerId, gameId);
        }
        playerRoundCards.put(gameId, gameCards);
    }

    public List<RoundCardDTO> getPlayerRoundCards(Long gameId, Long playerId) {
        return Optional.ofNullable(playerRoundCards.get(gameId))
                       .map(map -> map.getOrDefault(playerId, Collections.emptyList()))
                       .orElseGet(ArrayList::new);
    }

    public boolean removeRoundCardFromPlayer(Long gameId, Long playerId, String cardId) {
        Map<Long, List<RoundCardDTO>> inventories =
            playerRoundCards.computeIfAbsent(gameId, k -> new HashMap<>());
        List<RoundCardDTO> playerCards = inventories.get(playerId);
        if (playerCards == null) {
            log.warn("No ID‑based inventory for player {} in game {}", playerId, gameId);
            return false;
        }
        log.info("Before ID removal, player {} cards: {}", playerId,
                 playerCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(",")));
        boolean removed = playerCards.removeIf(c -> c.getId().equals(cardId));
        log.info("After ID removal, player {} has {} cards", playerId, playerCards.size());
        return removed;
    }

    // ----------------------------------------------------------------
    // TOKEN‑BASED FLOW
    // ----------------------------------------------------------------

    /**
     * Remove by token, with fallback to ID‑based if the token list is missing or empty.
     */
    public boolean removeRoundCardFromPlayerByToken(Long gameId, String playerToken, String cardId) {
        Map<String, List<RoundCardDTO>> gameTokenMap =
            tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());

        List<RoundCardDTO> tokenCards = gameTokenMap.get(playerToken);
        if (tokenCards != null && !tokenCards.isEmpty()) {
            log.info("Before token removal, token {} cards: {}", playerToken,
                     tokenCards.stream().map(RoundCardDTO::getId).collect(Collectors.joining(",")));
            boolean removed = tokenCards.removeIf(c -> c.getId().equals(cardId));
            log.info("After token removal, token {} has {} cards", playerToken, tokenCards.size());
            return removed;
        }

        // Fallback → ID‑based
        User user = authService.getUserByToken(playerToken);
        if (user != null) {
            return removeRoundCardFromPlayer(gameId, user.getId(), cardId);
        }
        log.warn("Cannot remove token card: no user for token {}", playerToken);
        return false;
    }

    /**
     * Check if a player has NO cards by token, falling back to ID‑based if needed.
     */
    public boolean hasNoRoundCardsByToken(Long gameId, String playerToken) {
        Map<String, List<RoundCardDTO>> gameTokenMap = tokenRoundCards.get(gameId);
        boolean tokenEmpty = gameTokenMap == null
                          || gameTokenMap.getOrDefault(playerToken, Collections.emptyList()).isEmpty();
        if (!tokenEmpty) return false;

        // Fallback → ID‑based
        User user = authService.getUserByToken(playerToken);
        if (user != null) {
            return getPlayerRoundCards(gameId, user.getId()).isEmpty();
        }
        return true;
    }

    /**
     * Assign two fresh cards to a user (token‑only, not persisted).
     */
    public List<RoundCardDTO> assignRoundCardsToPlayer(String userToken) {
        User user = authService.getUserByToken(userToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid token " + userToken);
        }
        List<RoundCardDTO> cards = new ArrayList<>();

        String prefix = userToken.substring(0, Math.min(8, userToken.length()));

        cards.add(createRoundCard(
            "world-" + prefix,
            "World",
            "The round includes the full available coverage",
            60
        ));
        cards.add(createRoundCard(
            "flash-" + prefix,
            "Flash",
            "The round includes the full available coverage, but the round time is halved",
            30
        ));

        return cards;
    }

    /**
     * Persist token‑based cards for a game (used by GameService.initializeGame).
     */
    public List<RoundCardDTO> assignPlayerRoundCards(Long gameId, String userToken) {
        List<RoundCardDTO> cards = assignRoundCardsToPlayer(userToken);
        tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>())
                       .put(userToken, new ArrayList<>(cards));
        log.info("Stored {} cards for game {} token {}",
                 cards.size(), gameId, userToken);
        return cards;
    }

    /**
     * Retrieve token‑based cards, with ID‑based fallback if none, or generate if neither.
     */
    public List<RoundCardDTO> getPlayerRoundCardsByToken(Long gameId, String userToken) {
        User user = authService.getUserByToken(userToken);
        if (user == null) {
            throw new IllegalArgumentException("Invalid token " + userToken);
        }

        Map<String, List<RoundCardDTO>> gameTokenMap =
            tokenRoundCards.computeIfAbsent(gameId, k -> new ConcurrentHashMap<>());
        List<RoundCardDTO> cards = gameTokenMap.get(userToken);

        // 1) Token list present?
        if (cards != null && !cards.isEmpty()) {
            return cards;
        }

        // 2) ID‑based fallback?
        List<RoundCardDTO> idCards = getPlayerRoundCards(gameId, user.getId());
        if (!idCards.isEmpty()) {
            gameTokenMap.put(userToken, new ArrayList<>(idCards));
            return idCards;
        }

        // 3) Generate fresh
        return assignPlayerRoundCards(gameId, userToken);
    }

    /**
     * @deprecated Kept for backward compatibility with GameWebSocketController
     */
    public boolean removePlayerRoundCard(Long gameId, String playerToken, String cardId) {
        return removeRoundCardFromPlayerByToken(gameId, playerToken, cardId);
    }

    /**
     * Return all available global cards for the “/games/data” endpoint.
     */
    public List<RoundCardDTO> getAllRoundCards() {
        log.info("Getting all global round cards");
        List<RoundCardDTO> list = new ArrayList<>();
        for (String id : ROUND_CARDS) {
            RoundCardDTO dto = new RoundCardDTO();
            dto.setId(id);
            if ("world".equals(id)) {
                dto.setName("World");
                dto.setDescription("The round includes the full available coverage");
                RoundCardModifiers m = new RoundCardModifiers();
                m.setTime(60);
                dto.setModifiers(m);
            } else if ("flash".equals(id)) {
                dto.setName("Flash");
                dto.setDescription("The round includes the full available coverage, but the round time is halved");
                RoundCardModifiers m = new RoundCardModifiers();
                m.setTime(30);
                dto.setModifiers(m);
            }
            list.add(dto);
        }
        return list;
    }

    public List<String> getRoundCardIds() {
        return new ArrayList<>(ROUND_CARDS);
    }
}
