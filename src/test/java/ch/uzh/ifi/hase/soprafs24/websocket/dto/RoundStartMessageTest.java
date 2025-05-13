package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundStartMessageTest {

    @Test
    void testConstructorWithDefaultRoundTime() {
        int round = 1;
        double latitude = 10.0;
        double longitude = 20.0;
        RoundStartMessage message = new RoundStartMessage(round, latitude, longitude);

        assertEquals("ROUND_START", message.getType(), "Type should be 'ROUND_START'.");
        assertEquals(round, message.getRound(), "Round should be initialized by constructor.");
        assertEquals(latitude, message.getLatitude(), "Latitude should be initialized by constructor.");
        assertEquals(longitude, message.getLongitude(), "Longitude should be initialized by constructor.");
        assertEquals(30, message.getRoundTime(), "Default roundTime should be 30.");
    }

    @Test
    void testConstructorWithCustomRoundTime() {
        int round = 2;
        double latitude = 30.0;
        double longitude = 40.0;
        int customRoundTime = 45;
        RoundStartMessage message = new RoundStartMessage(round, latitude, longitude, customRoundTime);

        assertEquals("ROUND_START", message.getType(), "Type should be 'ROUND_START'.");
        assertEquals(round, message.getRound(), "Round should be initialized by constructor.");
        assertEquals(latitude, message.getLatitude(), "Latitude should be initialized by constructor.");
        assertEquals(longitude, message.getLongitude(), "Longitude should be initialized by constructor.");
        assertEquals(customRoundTime, message.getRoundTime(), "Custom roundTime should be initialized by constructor.");
    }

    @Test
    void testGettersWithVariousValues() {
        // Test with zero and negative values where applicable, though semantics might dictate positive values
        RoundStartMessage message1 = new RoundStartMessage(0, 0.0, 0.0, 0);
        assertEquals(0, message1.getRound());
        assertEquals(0.0, message1.getLatitude());
        assertEquals(0.0, message1.getLongitude());
        assertEquals(0, message1.getRoundTime());

        RoundStartMessage message2 = new RoundStartMessage(-1, -10.5, -20.5, -5); // Assuming these values are permissible by logic
        assertEquals(-1, message2.getRound());
        assertEquals(-10.5, message2.getLatitude());
        assertEquals(-20.5, message2.getLongitude());
        assertEquals(-5, message2.getRoundTime()); // Though roundTime is usually positive
    }
}