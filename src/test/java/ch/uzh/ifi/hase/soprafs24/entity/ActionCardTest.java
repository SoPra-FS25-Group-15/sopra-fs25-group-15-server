package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionCardTest {

    private ActionCard actionCard;
    private User owner;

    @BeforeEach
    void setUp() {
        actionCard = new ActionCard();
        owner = new User();
        owner.setId(1L);
        owner.setUsername("testUser");
    }

    @Test
    void testConstructor_Default() {
        ActionCard card = new ActionCard();
        assertNotNull(card);
        assertNull(card.getId());
        assertNull(card.getType());
        assertNull(card.getTitle());
        assertNull(card.getDescription());
        assertNull(card.getOwner());
    }

    @Test
    void testId() {
        String id = "7choices";
        actionCard.setId(id);
        assertEquals(id, actionCard.getId());
    }

    @Test
    void testType() {
        ActionCardType type = ActionCardType.POWERUP;
        actionCard.setType(type);
        assertEquals(type, actionCard.getType());
    }

    @Test
    void testTitle() {
        String title = "7 Choices";
        actionCard.setTitle(title);
        assertEquals(title, actionCard.getTitle());
    }

    @Test
    void testDescription() {
        String description = "Reveal the continent...";
        actionCard.setDescription(description);
        assertEquals(description, actionCard.getDescription());
    }

    @Test
    void testGetName() {
        String title = "Bad Sight";
        actionCard.setTitle(title);
        assertEquals(title, actionCard.getName());
    }

    @Test
    void testFullyPopulatedActionCard() {
        ActionCard card = new ActionCard();
        card.setId("badsight");
        card.setType(ActionCardType.PUNISHMENT);
        card.setTitle("Bad Sight");
        card.setDescription("This is a punishment card that affects vision");
        card.setOwner(owner);

        assertEquals("badsight", card.getId());
        assertEquals(ActionCardType.PUNISHMENT, card.getType());
        assertEquals("Bad Sight", card.getTitle());
        assertEquals("Bad Sight", card.getName());
        assertEquals("This is a punishment card that affects vision", card.getDescription());
        assertEquals(owner, card.getOwner());
    }

    @Test
    void testDataAnnotation() {
        // Test if Lombok @Data generates equals and hashCode
        ActionCard card1 = new ActionCard();
        card1.setId("card1");
        card1.setType(ActionCardType.POWERUP);
        card1.setTitle("Power Card");
        card1.setDescription("Description");
        card1.setOwner(owner);

        ActionCard card2 = new ActionCard();
        card2.setId("card1");
        card2.setType(ActionCardType.POWERUP);
        card2.setTitle("Power Card");
        card2.setDescription("Description");
        card2.setOwner(owner);

        ActionCard card3 = new ActionCard();
        card3.setId("card2");
        card3.setType(ActionCardType.PUNISHMENT);
        card3.setTitle("Punishment Card");
        card3.setDescription("Different Description");
        card3.setOwner(owner);

        assertEquals(card1, card2);
        assertNotEquals(card1, card3);
        assertEquals(card1.hashCode(), card2.hashCode());
        assertNotEquals(card1.hashCode(), card3.hashCode());
    }

    @Test
    void testToString() {
        actionCard.setId("testCard");
        actionCard.setType(ActionCardType.POWERUP);
        actionCard.setTitle("Test Card");
        actionCard.setDescription("Test Description");
        actionCard.setOwner(owner);

        String toString = actionCard.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("testCard"));
        assertTrue(toString.contains("POWERUP"));
        assertTrue(toString.contains("Test Card"));
        assertTrue(toString.contains("Test Description"));
    }

    @Test
    void testNullValues() {
        actionCard.setId(null);
        actionCard.setType(null);
        actionCard.setTitle(null);
        actionCard.setDescription(null);
        actionCard.setOwner(null);

        assertNull(actionCard.getId());
        assertNull(actionCard.getType());
        assertNull(actionCard.getTitle());
        assertNull(actionCard.getDescription());
        assertNull(actionCard.getOwner());
        assertNull(actionCard.getName());
    }

    @Test
    void testDifferentActionCardTypes() {
        ActionCard powerupCard = new ActionCard();
        powerupCard.setType(ActionCardType.POWERUP);

        ActionCard punishmentCard = new ActionCard();
        punishmentCard.setType(ActionCardType.PUNISHMENT);

        assertEquals(ActionCardType.POWERUP, powerupCard.getType());
        assertEquals(ActionCardType.PUNISHMENT, punishmentCard.getType());
        assertNotEquals(powerupCard.getType(), punishmentCard.getType());
    }
}