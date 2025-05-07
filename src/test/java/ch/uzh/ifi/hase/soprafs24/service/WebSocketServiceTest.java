package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

/**
 * Test class for the WebSocketService
 * Tests all methods for sending messages through WebSocket
 */
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    private Long testUserId;
    private Long testGameId;
    private String testDestination;
    private Object testPayload;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        
        testUserId = 123L;
        testGameId = 456L;
        testDestination = "/topic/game/status";
        testPayload = new TestPayload("Test message");
    }

    @Test
    void sendToUser_sendsMessageToSpecificUser() {
        
        webSocketService.sendToUser(testUserId, testDestination, testPayload);

        
        verify(messagingTemplate).convertAndSendToUser(
                testUserId.toString(),
                testDestination,
                testPayload
        );
    }

    @Test
    void sendToGame_sendsMessageToSpecificGame() {
        
        webSocketService.sendToGame(testGameId, testDestination, testPayload);

        
        verify(messagingTemplate).convertAndSend(
                testDestination,
                testPayload
        );
    }

    @Test
    void sendToAll_sendsMessageToAllUsers() {
        
        webSocketService.sendToAll(testDestination, testPayload);

        
        verify(messagingTemplate).convertAndSend(
                testDestination,
                testPayload
        );
    }

    @Test
    void multipleMessagesTest_ensureAllMessagesAreSent() {
        
        Object payload1 = new TestPayload("First message");
        Object payload2 = new TestPayload("Second message");
        String destination1 = "/topic/notifications";
        String destination2 = "/topic/updates";

        
        webSocketService.sendToUser(testUserId, destination1, payload1);

        
        webSocketService.sendToGame(testGameId, destination2, payload2);

        
        verify(messagingTemplate).convertAndSendToUser(
                testUserId.toString(),
                destination1,
                payload1
        );

        verify(messagingTemplate).convertAndSend(
                destination2,
                payload2
        );
    }

    @Test
    void nullPayloadTest_shouldStillSendMessage() {
        
        Object nullPayload = null;

        
        webSocketService.sendToAll(testDestination, nullPayload);

        
        verify(messagingTemplate).convertAndSend(
                testDestination,
                nullPayload
        );
    }

    
    private static class TestPayload {
        private final String message;

        public TestPayload(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "TestPayload{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }
}