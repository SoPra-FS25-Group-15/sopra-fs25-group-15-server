package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    // disable Cloud SQL auto‐config
    "spring.cloud.gcp.sql.enabled=false",
    // H2 in-memory
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    // Hibernate DDL + SQL logging
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=true",
    // dummy placeholders
    "jwt.secret=test-secret",
    "google.maps.api.key=TEST_KEY"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RoundCardServiceTest {

    @Autowired
    private RoundCardService roundCardService;

    @MockBean
    private AuthService authService;

    @MockBean
    private GameService gameService;

    private User testUser;
    private static final String TEST_TOKEN = "test-token";
    private static final Long TEST_GAME_ID = 1L;

    @BeforeEach
    void setUp() {
        // prepare a user stub for token lookups
        testUser = new User();
        // set id via setter or ReflectionTestUtils if private
        testUser.setId(1L);
        testUser.setToken(TEST_TOKEN);
        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(testUser);
    }

    @Test
    void assignInitialRoundCards_success() {
        List<Long> playerIds = Arrays.asList(1L, 2L);
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, playerIds);

        // the service stores cards in a nested map: gameId → (playerId → cards)
        @SuppressWarnings("unchecked")
        Map<Long, Map<Long, List<RoundCardDTO>>> allCards =
            (Map<Long, Map<Long, List<RoundCardDTO>>>)
            org.springframework.test.util.ReflectionTestUtils
                .getField(roundCardService, "playerRoundCards");

        assertNotNull(allCards);
        assertTrue(allCards.containsKey(TEST_GAME_ID));

        Map<Long, List<RoundCardDTO>> gameCards = allCards.get(TEST_GAME_ID);
        assertEquals(2, gameCards.size());

        for (Long pid : playerIds) {
            List<RoundCardDTO> cards = gameCards.get(pid);
            assertEquals(2, cards.size(), "Each player gets 2 cards");
            assertTrue(cards.stream().anyMatch(c -> c.getId().startsWith("world-")));
            assertTrue(cards.stream().anyMatch(c -> c.getId().startsWith("flash-")));
        }
    }

    @Test
    void getPlayerRoundCards_success() {
        long playerId = 1L;
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, List.of(playerId));
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);

        assertNotNull(cards);
        assertEquals(2, cards.size());
    }

    @Test
    void getPlayerRoundCards_noneAssigned() {
        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, 999L);
        assertNotNull(cards);
        assertTrue(cards.isEmpty());
    }

    @Test
    void removeRoundCardFromPlayer_success() {
        long playerId = 1L;
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, List.of(playerId));

        List<RoundCardDTO> cards = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);
        String toRemove = cards.get(0).getId();

        assertTrue(roundCardService.removeRoundCardFromPlayer(TEST_GAME_ID, playerId, toRemove));
        List<RoundCardDTO> remaining = roundCardService.getPlayerRoundCards(TEST_GAME_ID, playerId);
        assertEquals(1, remaining.size());
        assertFalse(remaining.stream().anyMatch(c -> c.getId().equals(toRemove)));
    }

    @Test
    void removeRoundCardFromPlayer_cardNotFound() {
        long playerId = 1L;
        roundCardService.assignInitialRoundCards(TEST_GAME_ID, List.of(playerId));
        assertFalse(roundCardService.removeRoundCardFromPlayer(TEST_GAME_ID, playerId, "not-a-card"));
    }

    @Test
    void removeRoundCardFromPlayerByToken_success() {
        List<RoundCardDTO> cards = roundCardService.assignPlayerRoundCards(TEST_GAME_ID, TEST_TOKEN);
        String id = cards.get(0).getId();
        assertTrue(roundCardService.removeRoundCardFromPlayerByToken(TEST_GAME_ID, TEST_TOKEN, id));
    }

    @Test
    void assignRoundCardsToPlayer_success() {
        List<RoundCardDTO> cards = roundCardService.assignRoundCardsToPlayer(TEST_TOKEN);
        assertEquals(2, cards.size());
        assertTrue(cards.stream().anyMatch(c -> "World".equals(c.getName())));
        assertTrue(cards.stream().anyMatch(c -> "Flash".equals(c.getName())));
    }

    @Test
    void hasNoRoundCardsByToken_true() {
        assertTrue(roundCardService.hasNoRoundCardsByToken(999L, TEST_TOKEN));
    }

    @Test
    void getAllRoundCards_success() {
        List<RoundCardDTO> cards = roundCardService.getAllRoundCards();
        assertEquals(2, cards.size());
        assertTrue(cards.stream().anyMatch(c -> c.getId().equals("world")));
        assertTrue(cards.stream().anyMatch(c -> c.getId().equals("flash")));
    }

    @Test
    void getRoundCardIds_success() {
        List<String> ids = roundCardService.getRoundCardIds();
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(List.of("world", "flash")));
    }
}
