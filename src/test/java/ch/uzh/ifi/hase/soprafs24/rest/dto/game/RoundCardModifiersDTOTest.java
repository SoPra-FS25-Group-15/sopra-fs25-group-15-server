package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class RoundCardModifiersDTOTest {
    
    private RoundCardModifiersDTO modifiersDTO;
    
    @BeforeEach
    void setUp() {
        modifiersDTO = new RoundCardModifiersDTO();
        modifiersDTO.setTime(60);
        modifiersDTO.setGuesses(3);
        modifiersDTO.setStreetview("Standard");
    }
    
    @Test
    void testBasicProperties() {
        assertEquals(60, modifiersDTO.getTime());
        assertEquals(3, modifiersDTO.getGuesses());
        assertEquals("Standard", modifiersDTO.getStreetview());
    }
    
    @Test
    void testPropertyUpdates() {
        // Update properties
        modifiersDTO.setTime(30);
        modifiersDTO.setGuesses(1);
        modifiersDTO.setStreetview("Limited");
        
        // Verify updates
        assertEquals(30, modifiersDTO.getTime());
        assertEquals(1, modifiersDTO.getGuesses());
        assertEquals("Limited", modifiersDTO.getStreetview());
    }
    
    @Test
    void testDifferentModifierScenarios() {
        // Scenario 1: Quick round with multiple guesses
        RoundCardModifiersDTO quickRound = new RoundCardModifiersDTO();
        quickRound.setTime(15);
        quickRound.setGuesses(5);
        quickRound.setStreetview("Standard");
        
        assertEquals(15, quickRound.getTime());
        assertEquals(5, quickRound.getGuesses());
        
        // Scenario 2: Long round with single precise guess
        RoundCardModifiersDTO preciseRound = new RoundCardModifiersDTO();
        preciseRound.setTime(120);
        preciseRound.setGuesses(1);
        preciseRound.setStreetview("Full");
        
        assertEquals(120, preciseRound.getTime());
        assertEquals(1, preciseRound.getGuesses());
        assertEquals("Full", preciseRound.getStreetview());
    }
    
    @Test
    void testExtremeCases() {
        // Test with zero values
        RoundCardModifiersDTO zeroValues = new RoundCardModifiersDTO();
        zeroValues.setTime(0);
        zeroValues.setGuesses(0);
        
        assertEquals(0, zeroValues.getTime());
        assertEquals(0, zeroValues.getGuesses());
        
        // Test with large values
        RoundCardModifiersDTO largeValues = new RoundCardModifiersDTO();
        largeValues.setTime(3600); // 1 hour
        largeValues.setGuesses(100);
        
        assertEquals(3600, largeValues.getTime());
        assertEquals(100, largeValues.getGuesses());
    }
}
