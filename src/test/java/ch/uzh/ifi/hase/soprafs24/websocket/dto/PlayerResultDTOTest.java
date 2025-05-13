package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerResultDTOTest {

    @Test
    void testConstructorAndGetters() {
        String username = "Player1";
        int totalScore = 150;
        List<GuessResultDTO> guesses = new ArrayList<>();
        guesses.add(new GuessResultDTO(100, 10));
        guesses.add(new GuessResultDTO(50, 5));

        PlayerResultDTO dto = new PlayerResultDTO(username, totalScore, guesses);

        assertEquals(username, dto.getUsername(), "getUsername should return the username set in the constructor.");
        assertEquals(totalScore, dto.getTotalScore(), "getTotalScore should return the totalScore set in the constructor.");
        assertEquals(guesses, dto.getGuesses(), "getGuesses should return the list of guesses set in the constructor.");
        assertNotNull(dto.getGuesses());
        assertEquals(2, dto.getGuesses().size());
    }

    @Test
    void testConstructorWithEmptyGuesses() {
        String username = "Player2";
        int totalScore = 0;
        List<GuessResultDTO> guesses = Collections.emptyList();

        PlayerResultDTO dto = new PlayerResultDTO(username, totalScore, guesses);

        assertEquals(username, dto.getUsername());
        assertEquals(totalScore, dto.getTotalScore());
        assertNotNull(dto.getGuesses(), "Guesses list should not be null even if empty.");
        assertTrue(dto.getGuesses().isEmpty(), "Guesses list should be empty.");
    }

    @Test
    void testConstructorWithNullGuesses() {
        String username = "Player3";
        int totalScore = 50;
        List<GuessResultDTO> guesses = null;

        PlayerResultDTO dto = new PlayerResultDTO(username, totalScore, guesses);

        assertEquals(username, dto.getUsername());
        assertEquals(totalScore, dto.getTotalScore());
        assertNull(dto.getGuesses(), "Guesses list should be null if null was passed to the constructor.");
    }

    @Test
    void testConstructorWithNegativeScore() {
        String username = "Player4";
        int totalScore = -10;
        List<GuessResultDTO> guesses = new ArrayList<>();

        PlayerResultDTO dto = new PlayerResultDTO(username, totalScore, guesses);
        assertEquals(totalScore, dto.getTotalScore(), "Total score should correctly handle negative values if applicable.");
    }
}