package ch.uzh.ifi.hase.soprafs24.config;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private AuthService authService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory broker for topics
        config.enableSimpleBroker("/topic");
        // Set prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
        // Set prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the WebSocket endpoints with SockJS for browser clients
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user")
               .setAllowedOrigins("http://localhost:8000") 
               .withSockJS();
        
        // Also register endpoints without SockJS for Postman testing
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user")
               .setAllowedOrigins("*"); // Allow from any origin for testing
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add an interceptor to authenticate users via token
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                // Only authenticate on connect
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract the Authorization header
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String authHeader = authorization.get(0);
                        String token = authHeader;
                        
                        // Handle both "Bearer token" format and raw token format
                        if (authHeader.startsWith("Bearer ")) {
                            token = authHeader.substring(7);
                        }
                        
                        try {
                            // Validate token and get user
                            User user = authService.getUserByToken(token);
                            
                            // Set authenticated user as Principal
                            accessor.setUser(new Principal() {
                                @Override
                                public String getName() {
                                    return user.getId().toString();
                                }
                            });
                            
                            // Log successful authentication
                            logger.info("WebSocket authentication successful for user ID: {}", user.getId());
                        } catch (Exception e) {
                            // Log authentication failure with reason
                            logger.warn("WebSocket authentication failed: {}", e.getMessage());
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
                        }
                    } else {
                        // Log missing authorization header
                        logger.warn("WebSocket connection attempt without authorization token");
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authorization token provided");
                    }
                }
                return message;
            }
        });
    }
}
