package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class RoundCardDTOTest {
    
    private RoundCardDTO roundCardDTO;
    private RoundCardDTO.RoundCardModifiers modifiers;
    
    @BeforeEach
    void setUp() {
        roundCardDTO = new RoundCardDTO();
        roundCardDTO.setId("world-card");
        roundCardDTO.setName("World Map");
        roundCardDTO.setDescription("Standard world map with no restrictions");
        
        modifiers = new RoundCardDTO.RoundCardModifiers();
        modifiers.setGuessType("Precise");
        modifiers.setStreetView("Standard");
        modifiers.setTime(60);
        
        roundCardDTO.setModifiers(modifiers);
    }
    
    @Test
    void testRoundCardBasicProperties() {
        assertEquals("world-card", roundCardDTO.getId());
        assertEquals("World Map", roundCardDTO.getName());
        assertEquals("Standard world map with no restrictions", roundCardDTO.getDescription());
        assertNotNull(roundCardDTO.getModifiers());
    }
    
    @Test
    void testRoundCardModifiers() {
        assertEquals("Precise", roundCardDTO.getModifiers().getGuessType());
        assertEquals("Standard", roundCardDTO.getModifiers().getStreetView());
        assertEquals(60, roundCardDTO.getModifiers().getTime());
    }
    
    @Test
    void testModifiersUpdates() {
        // Update modifier values
        RoundCardDTO.RoundCardModifiers newModifiers = new RoundCardDTO.RoundCardModifiers();
        newModifiers.setGuessType("Area");
        newModifiers.setStreetView("Limited");
        newModifiers.setTime(30);
        
        roundCardDTO.setModifiers(newModifiers);
        
        // Verify updates
        assertEquals("Area", roundCardDTO.getModifiers().getGuessType());
        assertEquals("Limited", roundCardDTO.getModifiers().getStreetView());
        assertEquals(30, roundCardDTO.getModifiers().getTime());
    }
    
    @Test
    void testNullModifiers() {
        RoundCardDTO emptyCard = new RoundCardDTO();
        
        // Initial state should have null modifiers
        assertNull(emptyCard.getModifiers());
        
        // Setting modifiers
        emptyCard.setModifiers(modifiers);
        assertNotNull(emptyCard.getModifiers());
        
        // Setting modifiers to null
        emptyCard.setModifiers(null);
        assertNull(emptyCard.getModifiers());
    }
    
    @Test
    void testRoundCardWithDifferentSettings() {
        // Create a round card with different settings
        RoundCardDTO customCard = new RoundCardDTO();
        customCard.setId("europe-card");
        customCard.setName("Europe Map");
        customCard.setDescription("Map limited to European countries");
        
        RoundCardDTO.RoundCardModifiers europeModifiers = new RoundCardDTO.RoundCardModifiers();
        europeModifiers.setGuessType("Country");
        europeModifiers.setStreetView("No");
        europeModifiers.setTime(45);
        
        customCard.setModifiers(europeModifiers);
        
        // Assertions
        assertEquals("europe-card", customCard.getId());
        assertEquals("Europe Map", customCard.getName());
        assertEquals("Country", customCard.getModifiers().getGuessType());
        assertEquals("No", customCard.getModifiers().getStreetView());
        assertEquals(45, customCard.getModifiers().getTime());
    }
}
