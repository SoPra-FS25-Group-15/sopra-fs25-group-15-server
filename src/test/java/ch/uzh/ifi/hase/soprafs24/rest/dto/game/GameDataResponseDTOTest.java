package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GameDataResponseDTOTest {
    
    private GameDataResponseDTO gameDataResponseDTO;
    private List<RoundCardDTO> roundCards;
    private Map<Long, List<ActionCardDTO>> actionCards;
    
    @BeforeEach
    void setUp() {
        // Setup round cards
        roundCards = new ArrayList<>();
        
        RoundCardDTO card1 = new RoundCardDTO();
        card1.setId("world-card");
        card1.setName("World Map");
        
        RoundCardDTO.RoundCardModifiers modifiers1 = new RoundCardDTO.RoundCardModifiers();
        modifiers1.setGuessType("Precise");
        modifiers1.setStreetView("Standard");
        modifiers1.setTime(60);
        card1.setModifiers(modifiers1);
        
        RoundCardDTO card2 = new RoundCardDTO();
        card2.setId("europe-card");
        card2.setName("Europe Map");
        
        RoundCardDTO.RoundCardModifiers modifiers2 = new RoundCardDTO.RoundCardModifiers();
        modifiers2.setGuessType("Country");
        modifiers2.setStreetView("Limited");
        modifiers2.setTime(45);
        card2.setModifiers(modifiers2);
        
        roundCards.add(card1);
        roundCards.add(card2);
        
        // Setup action cards
        actionCards = new HashMap<>();
        
        ActionCardDTO powerupCard = new ActionCardDTO();
        powerupCard.setId("7choices");
        powerupCard.setType("powerup");
        powerupCard.setTitle("7 Choices");
        powerupCard.setDescription("Reveal the continent");
        
        ActionCardDTO punishmentCard = new ActionCardDTO();
        punishmentCard.setId("badsight");
        punishmentCard.setType("punishment");
        punishmentCard.setTitle("Bad Sight");
        punishmentCard.setDescription("Apply blur effect");
        
        // Player 1 has a powerup card
        List<ActionCardDTO> player1Cards = new ArrayList<>();
        player1Cards.add(powerupCard);
        actionCards.put(1L, player1Cards);
        
        // Player 2 has a punishment card
        List<ActionCardDTO> player2Cards = new ArrayList<>();
        player2Cards.add(punishmentCard);
        actionCards.put(2L, player2Cards);
        
        // Create and setup the DTO
        gameDataResponseDTO = new GameDataResponseDTO();
        gameDataResponseDTO.setRoundCards(roundCards);
        gameDataResponseDTO.setActionCards(actionCards);
    }
    
    @Test
    void testGetRoundCards() {
        List<RoundCardDTO> retrievedCards = gameDataResponseDTO.getRoundCards();
        
        assertNotNull(retrievedCards);
        assertEquals(2, retrievedCards.size());
        assertEquals("world-card", retrievedCards.get(0).getId());
        assertEquals("europe-card", retrievedCards.get(1).getId());
    }
    
    @Test
    void testGetActionCards() {
        Map<Long, List<ActionCardDTO>> retrievedCards = gameDataResponseDTO.getActionCards();
        
        assertNotNull(retrievedCards);
        assertEquals(2, retrievedCards.size());
        
        // Check player 1's cards
        assertTrue(retrievedCards.containsKey(1L));
        assertEquals(1, retrievedCards.get(1L).size());
        assertEquals("7choices", retrievedCards.get(1L).get(0).getId());
        
        // Check player 2's cards
        assertTrue(retrievedCards.containsKey(2L));
        assertEquals(1, retrievedCards.get(2L).size());
        assertEquals("badsight", retrievedCards.get(2L).get(0).getId());
    }
    
    @Test
    void testModifyRoundCards() {
        // Create a new round card
        RoundCardDTO newCard = new RoundCardDTO();
        newCard.setId("asia-card");
        newCard.setName("Asia Map");
        
        // Create a new list with the new card
        List<RoundCardDTO> updatedRoundCards = new ArrayList<>(roundCards);
        updatedRoundCards.add(newCard);
        
        // Update the DTO
        gameDataResponseDTO.setRoundCards(updatedRoundCards);
        
        // Verify the update
        List<RoundCardDTO> retrievedCards = gameDataResponseDTO.getRoundCards();
        assertEquals(3, retrievedCards.size());
        assertEquals("asia-card", retrievedCards.get(2).getId());
    }
    
    @Test
    void testModifyActionCards() {
        // Create a new action card
        ActionCardDTO newCard = new ActionCardDTO();
        newCard.setId("zoom");
        newCard.setType("powerup");
        newCard.setTitle("Zoom");
        
        // Add card to player 1
        actionCards.get(1L).add(newCard);
        
        // Create cards for a new player
        List<ActionCardDTO> player3Cards = new ArrayList<>();
        ActionCardDTO player3Card = new ActionCardDTO();
        player3Card.setId("timeplus");
        player3Card.setType("powerup");
        player3Cards.add(player3Card);
        
        // Add new player with card
        actionCards.put(3L, player3Cards);
        
        // Update the DTO (not needed as we're modifying the map reference)
        
        // Verify the updates
        Map<Long, List<ActionCardDTO>> retrievedCards = gameDataResponseDTO.getActionCards();
        
        // Check player 1's updated cards
        assertEquals(2, retrievedCards.get(1L).size());
        assertEquals("zoom", retrievedCards.get(1L).get(1).getId());
        
        // Check new player 3's cards
        assertTrue(retrievedCards.containsKey(3L));
        assertEquals(1, retrievedCards.get(3L).size());
        assertEquals("timeplus", retrievedCards.get(3L).get(0).getId());
    }
    
    @Test
    void testEmptyDTO() {
        GameDataResponseDTO emptyDTO = new GameDataResponseDTO();
        
        assertNull(emptyDTO.getRoundCards());
        assertNull(emptyDTO.getActionCards());
        
        // Set empty collections
        emptyDTO.setRoundCards(new ArrayList<>());
        emptyDTO.setActionCards(new HashMap<>());
        
        assertNotNull(emptyDTO.getRoundCards());
        assertTrue(emptyDTO.getRoundCards().isEmpty());
        
        assertNotNull(emptyDTO.getActionCards());
        assertTrue(emptyDTO.getActionCards().isEmpty());
    }
}
