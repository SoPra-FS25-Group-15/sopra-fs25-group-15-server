package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.RoundCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    // turn off the Cloud SQL post-processor
    "spring.cloud.gcp.sql.enabled=false"
})
@AutoConfigureTestDatabase(replace = ANY)
public class RoundCardRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoundCardRepository roundCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void findByOwner_shouldReturnCardsOwnedByUser() {
        // Create a user
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        user.setProfile(profile);
        User savedUser = entityManager.persistAndFlush(user);

        // Create round cards owned by this user
        RoundCard card1 = createRoundCard("world-card", "World", "World description", savedUser);
        RoundCard card2 = createRoundCard("flash-card", "Flash", "Flash description", savedUser);
        entityManager.persistAndFlush(card1);
        entityManager.persistAndFlush(card2);

        // Create a card not owned by this user
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setStatus(UserStatus.ONLINE);
        UserProfile otherProfile = new UserProfile();
        otherProfile.setUsername("otherUser");
        otherUser.setProfile(otherProfile);
        User savedOtherUser = entityManager.persistAndFlush(otherUser);
        
        RoundCard otherCard = createRoundCard("other-card", "Other", "Other description", savedOtherUser);
        entityManager.persistAndFlush(otherCard);

        // Test findByOwner
        List<RoundCard> foundCards = roundCardRepository.findByOwner(savedUser);
        
        // Assertions
        assertEquals(2, foundCards.size());
        assertTrue(foundCards.stream().anyMatch(card -> card.getId().equals("world-card")));
        assertTrue(foundCards.stream().anyMatch(card -> card.getId().equals("flash-card")));
        assertFalse(foundCards.stream().anyMatch(card -> card.getId().equals("other-card")));
    }

    @Test
    public void findById_shouldReturnCard() {
        // Create a user
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        user.setProfile(profile);
        User savedUser = entityManager.persistAndFlush(user);

        // Create round card
        RoundCard card = createRoundCard("test-card", "Test Card", "Test description", savedUser);
        entityManager.persistAndFlush(card);

        // Find by ID
        RoundCard foundCard = roundCardRepository.findById("test-card").orElse(null);
        
        // Assertions
        assertNotNull(foundCard);
        assertEquals("test-card", foundCard.getId());
        assertEquals("Test Card", foundCard.getTitle());
        assertEquals("Test description", foundCard.getDescription());
        assertEquals(savedUser, foundCard.getOwner());
    }
    
    @Test
    public void save_shouldPersistCard() {
        // Create a user
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        user.setProfile(profile);
        User savedUser = entityManager.persistAndFlush(user);
        
        // Create a card
        RoundCard card = createRoundCard("new-card", "New Card", "New description", savedUser);
        
        // Save through repository
        RoundCard savedCard = roundCardRepository.save(card);
        
        // Flush to ensure persistence
        entityManager.flush();
        
        // Find to verify
        RoundCard foundCard = entityManager.find(RoundCard.class, "new-card");
        
        // Assertions
        assertNotNull(foundCard);
        assertEquals("new-card", foundCard.getId());
        assertEquals("New Card", foundCard.getTitle());
        assertEquals(savedUser.getId(), foundCard.getOwner().getId());
    }

    private RoundCard createRoundCard(String id, String title, String description, User owner) {
        RoundCard card = new RoundCard();
        card.setId(id);
        card.setTitle(title);
        card.setDescription(description);
        card.setOwner(owner);
        
        RoundCardModifiers modifiers = new RoundCardModifiers();
        modifiers.setGuessType("Precise");
        modifiers.setStreetView("Standard");
        modifiers.setTime(60);
        card.setModifiers(modifiers);
        
        return card;
    }
}
