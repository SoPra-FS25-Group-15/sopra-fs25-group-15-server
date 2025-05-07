package ch.uzh.ifi.hase.soprafs24.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebSocketErrorHandlerTest {

    private WebSocketErrorHandler errorHandler;
    private Message<byte[]> clientMessage;
    private StompHeaderAccessor clientHeaderAccessor;

    @BeforeEach
    void setUp() {
        errorHandler = new WebSocketErrorHandler();

        // Set up client message headers
        clientHeaderAccessor = StompHeaderAccessor.create(StompCommand.SEND);
        clientHeaderAccessor.setDestination("/app/test");
        clientHeaderAccessor.setSessionId("test-session-123");
        clientHeaderAccessor.setReceiptId("receipt-456");

        // Create client message
        clientMessage = MessageBuilder.createMessage(
                "Test message".getBytes(StandardCharsets.UTF_8),
                clientHeaderAccessor.getMessageHeaders());
    }

    @Test
    void handleInternal_withNullClientHeaderAccessor() {
        // given
        StompHeaderAccessor errorHeaderAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        byte[] errorPayload = "Error payload".getBytes(StandardCharsets.UTF_8);
        Exception cause = new RuntimeException("Error with null client header accessor");

        // when
        Message<byte[]> result = errorHandler.handleInternal(errorHeaderAccessor, errorPayload, cause, null);

        // then
        assertNotNull(result);
    }
}