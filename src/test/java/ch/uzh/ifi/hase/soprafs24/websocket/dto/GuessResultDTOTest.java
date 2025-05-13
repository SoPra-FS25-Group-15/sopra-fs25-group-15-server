package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GuessResultDTOTest {

    @Test
    void testConstructorAndGetters() {
        Integer guessValue = 75;
        Integer diffValue = 5;
        GuessResultDTO dto = new GuessResultDTO(guessValue, diffValue);

        assertEquals(guessValue, dto.getGuess(), "getGuess should return the guess value set in the constructor.");
        assertEquals(diffValue, dto.getDiff(), "getDiff should return the difference value set in the constructor.");
    }

    @Test
    void testConstructorWithNegativeDiff() {
        Integer guessValue = 20;
        Integer diffValue = -10; // Difference could be negative if guess is higher than target
        GuessResultDTO dto = new GuessResultDTO(guessValue, diffValue);

        assertEquals(guessValue, dto.getGuess());
        assertEquals(diffValue, dto.getDiff(), "getDiff should correctly handle negative difference values.");
    }

    @Test
    void testConstructorWithZeroDiff() {
        Integer guessValue = 50;
        Integer diffValue = 0; // Perfect guess
        GuessResultDTO dto = new GuessResultDTO(guessValue, diffValue);

        assertEquals(guessValue, dto.getGuess());
        assertEquals(diffValue, dto.getDiff(), "getDiff should correctly handle zero difference (perfect guess).");
    }

    @Test
    void testConstructorWithNullValues() {
        // Assuming Integer can be null for these fields based on type.
        // If they must not be null, the constructor should have checks.
        Integer guessValue = null;
        Integer diffValue = null;
        GuessResultDTO dto = new GuessResultDTO(guessValue, diffValue);

        assertNull(dto.getGuess(), "getGuess should return null if null was passed for guess.");
        assertNull(dto.getDiff(), "getDiff should return null if null was passed for diff.");
    }

    @Test
    void testConstructorWithMixedNullValues() {
        Integer guessValue = 100;
        Integer diffValue = null;
        GuessResultDTO dto = new GuessResultDTO(guessValue, diffValue);

        assertEquals(guessValue, dto.getGuess());
        assertNull(dto.getDiff(), "getDiff should return null if null was passed, even if guess is not null.");

        GuessResultDTO dto2 = new GuessResultDTO(null, 15);
        assertNull(dto2.getGuess(), "getGuess should return null if null was passed, even if diff is not null.");
        assertEquals(Integer.valueOf(15), dto2.getDiff());
    }
}