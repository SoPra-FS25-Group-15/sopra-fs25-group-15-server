package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketErrorHandler;

@TestConfiguration
public class TestConfig {

    /**
     * Provide a mock WebSocketErrorHandler for testing
     */
    @Bean
    @Primary
    public WebSocketErrorHandler webSocketErrorHandler() {
        return new WebSocketErrorHandler();
    }
    
    /**
     * For tests where we just need a bean but not the implementation
     */
    @Bean
    @Primary
    public SimpMessagingTemplate messagingTemplate() {
        return org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    }
}
