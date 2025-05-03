package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

import java.util.Arrays;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameRoundService.RoundData;
import ch.uzh.ifi.hase.soprafs24.service.GameService.GameState;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;

@SpringBootTest(properties = {
    // disable GCP Cloud SQL auto‐configuration
    "spring.cloud.gcp.sql.enabled=false",
    // use in-memory H2
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    // Hibernate DDL & SQL logging
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=true",
    // dummy placeholders for any @Value injections
    "jwt.secret=test-secret",
    "google.maps.api.key=TEST_KEY"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
public class GameRoundServiceTest {

    private static final Long GAME_ID       = 1L;
    private static final String USER_TOKEN_1 = "user1-token";
    private static final String USER_TOKEN_2 = "user2-token";

    @Autowired
    private GameRoundService gameRoundService;

    @MockBean
    private ActionCardService actionCardService;

    @MockBean
    private GameService gameService;

    @MockBean
    private GoogleMapsService googleMapsService;

    @MockBean
    private AuthService authService;

    @MockBean
    private ActionCardMapper actionCardMapper;

    private List<String> playerTokens;
    private User testUser1;
    private User testUser2;
    private ActionCardDTO testActionCard;

    @BeforeEach
    public void setUp() {
        // --- create two users ---
        testUser1 = new User();
        testUser1.setId(1L);
        testUser1.setToken(USER_TOKEN_1);
        UserProfile p1 = new UserProfile();
        p1.setUsername("user1");
        testUser1.setProfile(p1);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setToken(USER_TOKEN_2);
        UserProfile p2 = new UserProfile();
        p2.setUsername("user2");
        testUser2.setProfile(p2);

        playerTokens = Arrays.asList(USER_TOKEN_1, USER_TOKEN_2);

        // --- action card stub ---
        testActionCard = new ActionCardDTO();
        testActionCard.setId("action-card-1");
        testActionCard.setType("powerup");
        testActionCard.setTitle("Test Card");
        testActionCard.setDescription("Test description");

        // --- stub AuthService ---
        when(authService.getUserByToken(USER_TOKEN_1)).thenReturn(testUser1);
        when(authService.getUserByToken(USER_TOKEN_2)).thenReturn(testUser2);

        // --- stub ActionCardService & Maps ---
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard);
        when(googleMapsService.getRandomCoordinatesOnLand(GAME_ID))
            .thenReturn(new LatLngDTO(10.0, 20.0));

        // --- stub GameService with a fake GameState ---
        GameState mockGameState = mock(GameState.class);
        when(gameService.getGameState(GAME_ID)).thenReturn(mockGameState);
        when(mockGameState.getInventoryForPlayer(anyString()))
            .thenReturn(new GameState.PlayerInventory());
    }

    @Test
    public void startGame_success() {
        gameRoundService.startGame(GAME_ID, playerTokens);
        // after startGame, round index = 0, so hasMoreRounds returns true
        assertTrue(gameRoundService.hasMoreRounds(GAME_ID));
    }

    @Test
    public void startNextRound_success() {
        gameRoundService.startGame(GAME_ID, playerTokens);
        RoundData roundData = gameRoundService.startNextRound(GAME_ID, playerTokens);

        assertNotNull(roundData);
        assertEquals(1, roundData.getRoundNumber());
        assertEquals(10.0, roundData.getLatitude());
        assertEquals(20.0, roundData.getLongitude());
        assertEquals(1, roundData.getGuesses());
    }

    @Test
    public void distributeFreshActionCardsByToken_success() {
        var result = gameRoundService.distributeFreshActionCardsByToken(GAME_ID, playerTokens);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("action-card-1", result.get(USER_TOKEN_1).getId());
        assertEquals("action-card-1", result.get(USER_TOKEN_2).getId());
        verify(actionCardService, times(2)).drawRandomCard();
    }

    @Test
    public void replacePlayerActionCardByToken_success() {
        // establish initial cards
        gameRoundService.distributeFreshActionCardsByToken(GAME_ID, playerTokens);
        ActionCardDTO replaced = gameRoundService.replacePlayerActionCardByToken(GAME_ID, USER_TOKEN_1);

        assertNotNull(replaced);
        assertEquals("action-card-1", replaced.getId());
        // two draws for initial + one for replace
        verify(actionCardService, times(3)).drawRandomCard();
    }

    @Test
    public void hasMoreRounds_true() {
        gameRoundService.startGame(GAME_ID, playerTokens);
        gameRoundService.startNextRound(GAME_ID, playerTokens);
        assertTrue(gameRoundService.hasMoreRounds(GAME_ID));
    }

    @Test
    public void hasMoreRounds_false() {
        gameRoundService.startGame(GAME_ID, playerTokens);
        // play all 3 rounds
        gameRoundService.startNextRound(GAME_ID, playerTokens);
        gameRoundService.startNextRound(GAME_ID, playerTokens);
        gameRoundService.startNextRound(GAME_ID, playerTokens);
        assertFalse(gameRoundService.hasMoreRounds(GAME_ID));
    }
}
