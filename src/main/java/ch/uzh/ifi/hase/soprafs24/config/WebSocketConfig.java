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
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

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
        // Create a handshake interceptor to enforce token authentication
        HandshakeInterceptor authInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
                
                // Get Authorization header
                List<String> authHeaders = request.getHeaders().get("Authorization");
                
                // Reject if no Authorization header
                if (authHeaders == null || authHeaders.isEmpty()) {
                    logger.warn("WebSocket handshake rejected: No Authorization header");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
                
                String authHeader = authHeaders.get(0);
                String token = authHeader;
                
                // Handle Bearer token format
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
                
                try {
                    // Validate token
                    User user = authService.getUserByToken(token);
                    
                    // Store user information in attributes for later use
                    attributes.put("userId", user.getId());
                    attributes.put("username", user.getProfile().getUsername());
                    
                    logger.info("WebSocket handshake authorized for user: {}", user.getId());
                    return true;
                } catch (Exception e) {
                    logger.warn("WebSocket handshake rejected: Invalid token - {}", e.getMessage());
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                    WebSocketHandler wsHandler, Exception exception) {
                // Nothing to do after handshake
            }
        };
        
        // Custom handshake handler with additional principal setup
        DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, 
                    Map<String, Object> attributes) {
                
                // Create and return a Principal based on the userId attribute
                if (attributes.containsKey("userId")) {
                    final Long userId = (Long) attributes.get("userId");
                    return new Principal() {
                        @Override
                        public String getName() {
                            return userId.toString();
                        }
                    };
                }
                
                return null; // This should not happen due to our interceptor check
            }
        };
        
        // Register endpoints with SockJS for browser clients
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user")
               .setAllowedOrigins("http://localhost:8000")
               .addInterceptors(authInterceptor)
               .setHandshakeHandler(handshakeHandler)
               .withSockJS();
        
        // Also register endpoints without SockJS for Postman testing
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user")
               .setAllowedOrigins("*") // Allow from any origin for testing
               .addInterceptors(authInterceptor)
               .setHandshakeHandler(handshakeHandler);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add an interceptor to authenticate users via token at STOMP level
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
