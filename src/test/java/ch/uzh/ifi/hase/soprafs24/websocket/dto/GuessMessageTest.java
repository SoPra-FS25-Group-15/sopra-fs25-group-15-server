package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuessMessageTest {

    @Test
    void testDefaultConstructor() {
        GuessMessage message = new GuessMessage();
        assertEquals(0, message.getGuess(), "Default constructor should initialize guess to 0.");
    }

    @Test
    void testParameterizedConstructor() {
        int guessValue = 42;
        GuessMessage message = new GuessMessage(guessValue);
        assertEquals(guessValue, message.getGuess(), "Parameterized constructor should set the guess value.");
    }

    @Test
    void testSetAndGetGuess() {
        GuessMessage message = new GuessMessage();
        int guessValue = 100;
        message.setGuess(guessValue);
        assertEquals(guessValue, message.getGuess(), "getGuess should return the value set by setGuess.");
    }

    @Test
    void testSetAndGetGuessWithNegativeValue() {
        GuessMessage message = new GuessMessage();
        int guessValue = -5;
        message.setGuess(guessValue);
        assertEquals(guessValue, message.getGuess(), "getGuess should handle negative values correctly.");
    }

    @Test
    void testSetAndGetGuessWithZero() {
        // Test setting to zero explicitly after non-zero initialization
        GuessMessage message = new GuessMessage(50);
        int guessValue = 0;
        message.setGuess(guessValue);
        assertEquals(guessValue, message.getGuess(), "getGuess should allow setting the value to zero.");
    }
}