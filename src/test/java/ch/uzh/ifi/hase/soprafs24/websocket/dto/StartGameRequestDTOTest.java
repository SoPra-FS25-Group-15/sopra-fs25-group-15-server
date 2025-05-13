package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartGameRequestDTOTest {

    @Test
    void testInitialState() {
        StartGameRequestDTO dto = new StartGameRequestDTO();
        assertEquals(0, dto.getRoundTime(), "RoundTime should be 0 initially (default for int).");
    }

    @Test
    void testSetAndGetRoundTime() {
        StartGameRequestDTO dto = new StartGameRequestDTO();
        int roundTime = 90;
        dto.setRoundTime(roundTime);
        assertEquals(roundTime, dto.getRoundTime(), "getRoundTime should return the set round time.");
    }

    @Test
    void testSetRoundTimeWithZero() {
        StartGameRequestDTO dto = new StartGameRequestDTO();
        dto.setRoundTime(0);
        assertEquals(0, dto.getRoundTime(), "getRoundTime should return 0 if set to 0.");
    }

    @Test
    void testSetRoundTimeWithNegativeValue() {
        // Test DTO's ability to hold value, business logic for validation is separate.
        StartGameRequestDTO dto = new StartGameRequestDTO();
        dto.setRoundTime(-30);
        assertEquals(-30, dto.getRoundTime(), "getRoundTime should return a negative value if set.");
    }
}