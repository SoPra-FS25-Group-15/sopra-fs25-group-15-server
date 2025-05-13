package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameWinnerBroadcastTest {

    @Test
    void testConstructorAndGetters() {
        String winner = "PlayerOne";
        GameWinnerBroadcast broadcast = new GameWinnerBroadcast(winner);

        assertEquals("GAME_WINNER", broadcast.getType(), "The type should always be 'GAME_WINNER'.");
        assertEquals(winner, broadcast.getWinnerUsername(), "getWinnerUsername should return the username set in the constructor.");
    }

    @Test
    void testConstructorWithDifferentWinner() {
        String winner = "AnotherWinner";
        GameWinnerBroadcast broadcast = new GameWinnerBroadcast(winner);

        assertEquals("GAME_WINNER", broadcast.getType()); // Still check type
        assertEquals(winner, broadcast.getWinnerUsername(), "getWinnerUsername should correctly reflect a different winner's name.");
    }

    @Test
    void testConstructorWithEmptyWinnerName() {
        String winner = "";
        GameWinnerBroadcast broadcast = new GameWinnerBroadcast(winner);

        assertEquals("GAME_WINNER", broadcast.getType());
        assertEquals(winner, broadcast.getWinnerUsername(), "getWinnerUsername should allow an empty string for winner's name if intended.");
    }

    @Test
    void testConstructorWithNullWinnerName() {
        // Depending on desired behavior, null might be acceptable or throw an error.
        // If it should not be null, the constructor might need a null check.
        // For this test, we assume null is stored as is.
        String winner = null;
        GameWinnerBroadcast broadcast = new GameWinnerBroadcast(winner);

        assertEquals("GAME_WINNER", broadcast.getType());
        assertNull(broadcast.getWinnerUsername(), "getWinnerUsername should return null if null was passed to the constructor.");
    }
}