package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true",
        "jwt.secret=test-secret",
        "google.maps.api.key=TEST_KEY"
})
@ActiveProfiles("test")
public class ActionCardServiceIntegrationTest {

    @Autowired
    private ActionCardService actionCardService;

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    private User player1;
    private User player2;
    private Game testGame;
    private String player1Token;
    private String player2Token;

    @BeforeEach
    public void setup() {
        // Clear all repositories
        gameRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        player1 = createTestUser("player1@example.com", "password1");
        player2 = createTestUser("player2@example.com", "password2");

        // Generate tokens for the players
        player1Token = player1.getToken();
        player2Token = player2.getToken();

        // Create a test game with the players
        testGame = createTestGame(player1, player2);
    }

    private User createTestUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setStatus(UserStatus.ONLINE);
        user.generateToken();

        // Create and associate a profile
        UserProfile profile = new UserProfile();
        profile.setUsername(email.split("@")[0]);
        user.setProfile(profile);

        return userRepository.save(user);
    }

    private Game createTestGame(User host, User guest) {
        Game game = new Game();
        game.setName("TestGame-" + System.currentTimeMillis());
        game.setCreatorUsername(host.getProfile().getUsername());
        game.setCreationDate(LocalDateTime.now());
        game.setMaxPlayers(4);
        game.setStatus(GameStatus.LOBBY);

        // Add players to the game
        Set<User> players = new HashSet<>();
        players.add(host);
        players.add(guest);
        game.setPlayers(players);

        return gameRepository.save(game);
    }

    @Test
    public void drawRandomCard_returnsValidCard() {
        // Act
        ActionCardDTO card = actionCardService.drawRandomCard();

        // Assert
        assertNotNull(card);
        assertNotNull(card.getId());
        assertNotNull(card.getType());
        assertNotNull(card.getTitle());
        assertNotNull(card.getDescription());

        // Verify the card is one of the predefined cards
        boolean isValidCard = "7choices".equals(card.getId()) || "badsight".equals(card.getId());
        assertTrue(isValidCard);
    }

    @Test
    public void findById_existingCard_returnsCard() {
        // Act
        ActionCardDTO card = actionCardService.findById("7choices");

        // Assert
        assertNotNull(card);
        assertEquals("7choices", card.getId());
        assertEquals("powerup", card.getType());
        assertEquals("7 Choices", card.getTitle());
    }

    @Test
    public void findById_nonExistingCard_returnsNull() {
        // Act
        ActionCardDTO card = actionCardService.findById("nonexistent");

        // Assert
        assertNull(card);
    }

    @Test
    public void isValidActionCard_existingCards_returnsTrue() {
        // Act & Assert
        assertTrue(actionCardService.isValidActionCard("7choices"));
        assertTrue(actionCardService.isValidActionCard("badsight"));
    }

    @Test
    public void isValidActionCard_nonExistingCard_returnsFalse() {
        // Act & Assert
        assertFalse(actionCardService.isValidActionCard("nonexistent"));
    }

    @Test
    public void processActionCardEffect_7choices_returnsCorrectEffect() {
        // Act
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("7choices", player2Token);

        // Assert
        assertNotNull(effect);
        assertEquals("continent", effect.getEffectType());
        assertNull(effect.getTargetPlayer());
    }

    @Test
    public void processActionCardEffect_badsight_returnsCorrectEffect() {
        // Act
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("badsight", player2Token);

        // Assert
        assertNotNull(effect);
        assertEquals("blur", effect.getEffectType());
        assertEquals(player2Token, effect.getTargetPlayer());
    }

    @Test
    public void processActionCardEffect_invalidCard_returnsNull() {
        // Act
        ActionCardEffectDTO effect = actionCardService.processActionCardEffect("nonexistent", player2Token);

        // Assert
        assertNull(effect);
    }

    @Test
    public void processActionCardForGame_badsight_appliesEffectToTarget() {
        // Arrange
        Long gameId = testGame.getId();

        // Act
        ActionCardEffectDTO effect = actionCardService.processActionCardForGame(
                gameId, player1Token, "badsight", player2Token);

        // Assert
        assertNotNull(effect);
        assertEquals("blur", effect.getEffectType());
        assertEquals(player2Token, effect.getTargetPlayer());

    }

    @Test
    public void processActionCardForGame_7choices_appliesEffectToSelf() {
        // Arrange
        Long gameId = testGame.getId();

        // Act
        ActionCardEffectDTO effect = actionCardService.processActionCardForGame(
                gameId, player1Token, "7choices", player2Token);

        // Assert
        assertNotNull(effect);
        assertEquals("continent", effect.getEffectType());

        // Note: Additional assertions about the applied effect would go here
    }

    @Test
    public void getContinent_validCoordinates_returnsCorrectContinent() {
        // Test various coordinates for different continents
        assertEquals("Europe", actionCardService.getContinent(45.0, 10.0));
        assertEquals("Asia", actionCardService.getContinent(35.0, 100.0));
        assertEquals("Africa", actionCardService.getContinent(-10.0, 20.0));
        assertEquals("North America", actionCardService.getContinent(40.0, -100.0));
        assertEquals("South America", actionCardService.getContinent(-20.0, -60.0));
        assertEquals("Australia", actionCardService.getContinent(-25.0, 135.0));
        assertEquals("Antarctica", actionCardService.getContinent(-70.0, 0.0));
    }

    @Test
    public void getContinent_edgeCases_returnsExpectedContinent() {
        // Test edge cases or boundary coordinates
        assertEquals("Unknown", actionCardService.getContinent(0.0, 0.0));

        // Test extreme north
        assertEquals("North America", actionCardService.getContinent(80.0, -100.0));

        // Test international date line area
        assertEquals("Unknown", actionCardService.getContinent(0.0, 180.0));
    }

    @Test
    public void completeFunctionalFlow_cardDrawnAndPlayed() {
        // This test mimics a complete functional flow:
        // 1. Player draws a card
        // 2. Player plays the card
        // 3. Effect is processed

        // Arrange
        Long gameId = testGame.getId();

        // Act - Draw a card (we'll use findById since drawing is random)
        ActionCardDTO card = actionCardService.findById("badsight");

        // Play the card targeting another player
        ActionCardEffectDTO effect = actionCardService.processActionCardForGame(
                gameId, player1Token, card.getId(), player2Token);

        // Assert
        assertNotNull(effect);
        assertEquals("blur", effect.getEffectType());
        assertEquals(player2Token, effect.getTargetPlayer());
    }
}