package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartGameMessageTest {

    @Test
    void testDefaultConstructor() {
        StartGameMessage message = new StartGameMessage();
        assertEquals(0, message.getRoundCount(), "Default constructor should initialize roundCount to 0.");
        assertEquals(0, message.getRoundTime(), "Default constructor should initialize roundTime to 0.");
    }

    @Test
    void testParameterizedConstructor() {
        int roundCount = 10;
        int roundTime = 30;
        StartGameMessage message = new StartGameMessage(roundCount, roundTime);

        assertEquals(roundCount, message.getRoundCount(), "Parameterized constructor should set roundCount.");
        assertEquals(roundTime, message.getRoundTime(), "Parameterized constructor should set roundTime.");
    }

    @Test
    void testSetAndGetRoundCount() {
        StartGameMessage message = new StartGameMessage();
        int roundCount = 5;
        message.setRoundCount(roundCount);
        assertEquals(roundCount, message.getRoundCount(), "getRoundCount should return the set roundCount.");
    }

    @Test
    void testSetAndGetRoundTime() {
        StartGameMessage message = new StartGameMessage();
        int roundTime = 45;
        message.setRoundTime(roundTime);
        assertEquals(roundTime, message.getRoundTime(), "getRoundTime should return the set roundTime.");
    }

    @Test
    void testSettersWithNegativeValues() {
        // Assuming game logic might handle or prevent negative values elsewhere,
        // but DTO should allow setting them.
        StartGameMessage message = new StartGameMessage();
        message.setRoundCount(-1);
        assertEquals(-1, message.getRoundCount(), "setRoundCount should allow negative values.");
        message.setRoundTime(-10);
        assertEquals(-10, message.getRoundTime(), "setRoundTime should allow negative values.");
    }
}