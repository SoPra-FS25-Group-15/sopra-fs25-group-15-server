package ch.uzh.ifi.hase.soprafs24.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthService authService;

    public WebSocketConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple broker for destinations like /queue and /topic.
        config.enableSimpleBroker("/queue", "/topic");
        // Set the prefix used to route user-specific messages.
        config.setUserDestinationPrefix("/user");
        // Prefix for messages bound for methods annotated with @MessageMapping.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the endpoint for WebSocket connections.
        registry.addEndpoint("/ws-endpoint")
                .addInterceptors(new AuthHandshakeInterceptor(authService))
                .setHandshakeHandler(new CustomHandshakeHandler())
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
