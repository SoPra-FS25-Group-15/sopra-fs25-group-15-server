package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoundWinnerBroadcastTest {

    @Test
    void testOriginalConstructorAndGetters() {
        String winnerUsername = "Winner1";
        int round = 1;
        RoundWinnerBroadcast broadcast = new RoundWinnerBroadcast(winnerUsername, round);

        assertEquals("ROUND_WINNER", broadcast.getType(), "Type should be 'ROUND_WINNER'.");
        assertEquals(winnerUsername, broadcast.getWinnerUsername(), "WinnerUsername should be initialized by original constructor.");
        assertEquals(round, broadcast.getRound(), "Round should be initialized by original constructor.");
        assertEquals(0, broadcast.getDistance(), "Distance should default to 0 for original constructor.");
    }

    @Test
    void testNewConstructorAndGetters() {
        String winnerUsername = "Winner2";
        int round = 2;
        int distance = 500;
        RoundWinnerBroadcast broadcast = new RoundWinnerBroadcast(winnerUsername, round, distance);

        assertEquals("ROUND_WINNER", broadcast.getType(), "Type should be 'ROUND_WINNER'.");
        assertEquals(winnerUsername, broadcast.getWinnerUsername(), "WinnerUsername should be initialized by new constructor.");
        assertEquals(round, broadcast.getRound(), "Round should be initialized by new constructor.");
        assertEquals(distance, broadcast.getDistance(), "Distance should be initialized by new constructor.");
    }

    @Test
    void testNewConstructorWithZeroDistance() {
        String winnerUsername = "Winner3";
        int round = 3;
        int distance = 0;
        RoundWinnerBroadcast broadcast = new RoundWinnerBroadcast(winnerUsername, round, distance);

        assertEquals(winnerUsername, broadcast.getWinnerUsername());
        assertEquals(round, broadcast.getRound());
        assertEquals(distance, broadcast.getDistance(), "Distance should be 0 if set to 0 in new constructor.");
    }

    @Test
    void testConstructorWithNullWinner() {
        // Assuming null winner username is allowed and stored as null
        String winnerUsername = null;
        int round = 4;
        RoundWinnerBroadcast broadcast1 = new RoundWinnerBroadcast(winnerUsername, round);
        assertNull(broadcast1.getWinnerUsername(), "WinnerUsername should be null if passed as null (original constructor).");

        RoundWinnerBroadcast broadcast2 = new RoundWinnerBroadcast(winnerUsername, round, 100);
        assertNull(broadcast2.getWinnerUsername(), "WinnerUsername should be null if passed as null (new constructor).");
    }
}