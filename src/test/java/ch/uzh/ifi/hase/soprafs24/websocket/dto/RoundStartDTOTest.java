package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundStartDTOTest {

    @Test
    void testInitialState() {
        RoundStartDTO dto = new RoundStartDTO();
        assertEquals(0, dto.getRound(), "Round should be 0 initially (default for int).");
        assertEquals(0.0, dto.getLatitude(), "Latitude should be 0.0 initially (default for double).");
        assertEquals(0.0, dto.getLongitude(), "Longitude should be 0.0 initially (default for double).");
        assertEquals(0, dto.getRoundTime(), "RoundTime should be 0 initially (default for int).");
        assertTrue(dto.isStartTimer(), "StartTimer should be true by default.");
    }

    @Test
    void testSetAndGetRound() {
        RoundStartDTO dto = new RoundStartDTO();
        int round = 5;
        dto.setRound(round);
        assertEquals(round, dto.getRound(), "getRound should return the set round number.");
    }

    @Test
    void testSetAndGetLatitude() {
        RoundStartDTO dto = new RoundStartDTO();
        double latitude = 47.3769;
        dto.setLatitude(latitude);
        assertEquals(latitude, dto.getLatitude(), "getLatitude should return the set latitude.");
    }

    @Test
    void testSetAndGetLongitude() {
        RoundStartDTO dto = new RoundStartDTO();
        double longitude = 8.5417;
        dto.setLongitude(longitude);
        assertEquals(longitude, dto.getLongitude(), "getLongitude should return the set longitude.");
    }

    @Test
    void testSetAndGetRoundTime() {
        RoundStartDTO dto = new RoundStartDTO();
        int roundTime = 60;
        dto.setRoundTime(roundTime);
        assertEquals(roundTime, dto.getRoundTime(), "getRoundTime should return the set round time.");
    }

    @Test
    void testSetAndIsStartTimer() {
        RoundStartDTO dto = new RoundStartDTO();
        // Test setting to false, as default is true
        dto.setStartTimer(false);
        assertFalse(dto.isStartTimer(), "isStartTimer should return false after being set to false.");

        // Test setting back to true
        dto.setStartTimer(true);
        assertTrue(dto.isStartTimer(), "isStartTimer should return true after being set to true.");
    }
}