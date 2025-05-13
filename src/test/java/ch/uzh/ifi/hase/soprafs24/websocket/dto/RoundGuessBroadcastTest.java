package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoundGuessBroadcastTest {

    @Test
    void testConstructorAndGetters() {
        String username = "GuesserUser";
        Double latitude = 47.3769;  // Example Zurich latitude
        Double longitude = 8.5417; // Example Zurich longitude
        Integer distance = 1500;   // 1.5 km
        int round = 3;

        RoundGuessBroadcast broadcast = new RoundGuessBroadcast(username, latitude, longitude, distance, round);

        assertEquals("ROUND_GUESS", broadcast.getType(), "Type should always be 'ROUND_GUESS'.");
        assertEquals(username, broadcast.getUsername(), "getUsername should return the constructor-set username.");
        assertEquals(latitude, broadcast.getLatitude(), "getLatitude should return the constructor-set latitude.");
        assertEquals(longitude, broadcast.getLongitude(), "getLongitude should return the constructor-set longitude.");
        assertEquals(distance, broadcast.getDistance(), "getDistance should return the constructor-set distance.");
        assertEquals(round, broadcast.getRound(), "getRound should return the constructor-set round number.");
    }

    @Test
    void testConstructorWithNullOptionalValues() {
        String username = "OptionalGuesser";
        Double latitude = null;
        Double longitude = null;
        Integer distance = null;
        int round = 1;

        RoundGuessBroadcast broadcast = new RoundGuessBroadcast(username, latitude, longitude, distance, round);

        assertEquals("ROUND_GUESS", broadcast.getType());
        assertEquals(username, broadcast.getUsername());
        assertNull(broadcast.getLatitude(), "Latitude should be null if passed as null.");
        assertNull(broadcast.getLongitude(), "Longitude should be null if passed as null.");
        assertNull(broadcast.getDistance(), "Distance should be null if passed as null.");
        assertEquals(round, broadcast.getRound());
    }

    @Test
    void testConstructorWithZeroValues() {
        String username = "ZeroGuesser";
        Double latitude = 0.0;
        Double longitude = 0.0;
        Integer distance = 0;
        int round = 0;

        RoundGuessBroadcast broadcast = new RoundGuessBroadcast(username, latitude, longitude, distance, round);

        assertEquals(latitude, broadcast.getLatitude());
        assertEquals(longitude, broadcast.getLongitude());
        assertEquals(distance, broadcast.getDistance());
        assertEquals(round, broadcast.getRound());
    }
}