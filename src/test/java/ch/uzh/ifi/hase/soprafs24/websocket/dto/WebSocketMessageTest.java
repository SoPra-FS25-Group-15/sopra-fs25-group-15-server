package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketMessageTest {

    @Test
    void testDefaultConstructor() {
        WebSocketMessage<String> message = new WebSocketMessage<>();
        assertNull(message.getType(), "Default constructor should initialize type to null.");
        assertNull(message.getPayload(), "Default constructor should initialize payload to null.");
        assertNotNull(message.getTimestamp(), "Default constructor should initialize timestamp.");
    }

    @Test
    void testParameterizedConstructor() {
        String type = "TEST_MESSAGE";
        String payload = "Test Payload";
        WebSocketMessage<String> message = new WebSocketMessage<>(type, payload);

        assertEquals(type, message.getType(), "Type should be initialized by constructor.");
        assertEquals(payload, message.getPayload(), "Payload should be initialized by constructor.");
        assertNotNull(message.getTimestamp(), "Timestamp should be initialized by constructor.");
    }

    @Test
    void testSetAndGetType() {
        WebSocketMessage<String> message = new WebSocketMessage<>();
        String type = "UPDATE";
        message.setType(type);
        assertEquals(type, message.getType(), "Getter should return the set type.");
    }

    @Test
    void testSetAndGetPayload() {
        WebSocketMessage<Integer> message = new WebSocketMessage<>();
        Integer payload = 12345;
        message.setPayload(payload);
        assertEquals(payload, message.getPayload(), "Getter should return the set payload.");
    }

    @Test
    void testSetAndGetTimestamp() {
        WebSocketMessage<String> message = new WebSocketMessage<>();
        Instant specificTime = Instant.parse("2025-05-10T10:00:00Z");
        message.setTimestamp(specificTime);
        assertEquals(specificTime, message.getTimestamp(), "Getter should return the set timestamp.");
    }

    @Test
    void testTimestampIsSetOnCreation() {
        // Test that timestamp is set reasonably close to now.
        // Allow a small delta for execution time.
        Instant beforeCreation = Instant.now();
        WebSocketMessage<String> message = new WebSocketMessage<>("TYPE", "PAYLOAD");
        Instant afterCreation = Instant.now();

        assertTrue(message.getTimestamp().equals(beforeCreation) || message.getTimestamp().isAfter(beforeCreation),
                "Timestamp should be at or after the time of creation (before).");
        assertTrue(message.getTimestamp().equals(afterCreation) || message.getTimestamp().isBefore(afterCreation),
                "Timestamp should be at or before the time of creation (after).");
    }
}