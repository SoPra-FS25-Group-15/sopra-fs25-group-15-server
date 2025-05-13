package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RoundResultsMessageTest {

    @Test
    void testConstructorAndGetters_GameNotEnded() {
        boolean gameEnd = false;
        List<PlayerResultDTO> results = new ArrayList<>();
        PlayerResultDTO player1Result = mock(PlayerResultDTO.class);
        PlayerResultDTO player2Result = mock(PlayerResultDTO.class);
        results.add(player1Result);
        results.add(player2Result);

        RoundResultsMessage message = new RoundResultsMessage(gameEnd, results);

        assertEquals("ROUND_RESULTS", message.getType(), "Type should always be 'ROUND_RESULTS'.");
        assertEquals(gameEnd, message.isGameEnd(), "isGameEnd should reflect the constructor argument.");
        assertFalse(message.isGameEnd(), "Game should not be ended.");
        assertEquals(results, message.getResults(), "getResults should return the list from the constructor.");
        assertNotNull(message.getResults());
        assertEquals(2, message.getResults().size());
    }

    @Test
    void testConstructorAndGetters_GameEnded() {
        boolean gameEnd = true;
        List<PlayerResultDTO> results = Collections.singletonList(mock(PlayerResultDTO.class));

        RoundResultsMessage message = new RoundResultsMessage(gameEnd, results);

        assertEquals("ROUND_RESULTS", message.getType());
        assertTrue(message.isGameEnd(), "Game should be ended.");
        assertEquals(results, message.getResults());
        assertNotNull(message.getResults());
        assertEquals(1, message.getResults().size());
    }

    @Test
    void testConstructorWithEmptyResults() {
        boolean gameEnd = false;
        List<PlayerResultDTO> results = Collections.emptyList();

        RoundResultsMessage message = new RoundResultsMessage(gameEnd, results);

        assertEquals("ROUND_RESULTS", message.getType());
        assertFalse(message.isGameEnd());
        assertNotNull(message.getResults(), "Results list should not be null even if empty.");
        assertTrue(message.getResults().isEmpty(), "Results list should be empty.");
    }

    @Test
    void testConstructorWithNullResults() {
        boolean gameEnd = true;
        List<PlayerResultDTO> results = null;

        RoundResultsMessage message = new RoundResultsMessage(gameEnd, results);

        assertEquals("ROUND_RESULTS", message.getType());
        assertTrue(message.isGameEnd());
        assertNull(message.getResults(), "Results list should be null if passed as null to constructor.");
    }
}