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
        // Enable a simple in-memory broker with longer heartbeat intervals
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[] {10000, 10000}) // Increased heartbeat interval for stability
              .setTaskScheduler(new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler() {{
                  setPoolSize(1);
                  setThreadNamePrefix("websocket-heartbeat-thread-");
                  initialize();
              }});
        
        // Set prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
        
        logger.info("Message broker configured with /topic, /app prefixes and 10s heartbeat");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Create a handshake interceptor to enforce token authentication
        HandshakeInterceptor authInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
                
                logger.info("WebSocket handshake request received from: {}", request.getRemoteAddress());
                
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
                    attributes.put("token", token); // Store token for later use
                    
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
                if (exception != null) {
                    logger.error("Error during WebSocket handshake: {}", exception.getMessage());
                }
            }
        };
        
        // Custom handshake handler with additional principal setup
        DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, 
                    Map<String, Object> attributes) {
                
                // Create and return a Principal based on the userId attribute
                if (attributes.containsKey("userId")) {
                    // Fix type conversion issues with userId
                    final Long userId;
                    Object userIdObj = attributes.get("userId");
                    
                    if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }
                    
                    // Fix: Use concatenation instead of placeholder to avoid Throwable confusion
                    logger.info("Creating principal for user: " + userId);
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
        
        logger.info("Registering STOMP endpoints: /ws/lobby, /ws/lobby-manager, /ws/user", "/ws/friend");
        
        // Register endpoints with SockJS for browser clients
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend")
               .setAllowedOrigins("*") // Allow from all origins (change as needed)
               .addInterceptors(authInterceptor)
               .setHandshakeHandler(handshakeHandler)
               .withSockJS()
               .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
               .setWebSocketEnabled(true)
               .setDisconnectDelay(5000); // Set disconnect delay for testing
        
        // Also register endpoints without SockJS for Postman testing
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend")
               .setAllowedOrigins("*") // Allow from any origin for testing
               .addInterceptors(authInterceptor)
               .setHandshakeHandler(handshakeHandler);
        
        logger.info("STOMP endpoints registered successfully");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add an interceptor to authenticate users via token at STOMP level
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    
                    // Check for malformed messages
                    if (accessor == null || accessor.getCommand() == null) {
                        logger.error("Received malformed STOMP frame or non-STOMP message");
                        // Return the message as-is - the framework will reject it with appropriate error
                        return message;
                    }
                    
                    // Enhanced logging - log ALL messages for debugging
                    logger.info("STOMP command received: {}, sessionId={}, payload={}", 
                        accessor.getCommand(), accessor.getSessionId(), 
                        message.getPayload() != null ? message.getPayload().toString() : "null");
                    
                    // Log all headers for debugging
                    accessor.toNativeHeaderMap().forEach((key, value) -> {
                        logger.info("STOMP header: {} = {}", key, value);
                    });
                    
                    // Debug specific commands
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        logger.info("Processing STOMP CONNECT frame");
                        
                        // Get token from handshake attributes if available
                        String token = null;
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
                            token = (String)sessionAttributes.get("token");
                            // Fix: Check token length before substring to avoid errors
                            if (token.length() > 10) {
                                logger.info("Using token from handshake: {}", token.substring(0, 10) + "...");
                            } else {
                                logger.info("Using token from handshake: {}", token);
                            }
                        }
                        
                        // Fall back to Authorization header if needed
                        if (token == null) {
                            List<String> authorization = accessor.getNativeHeader("Authorization");
                            if (authorization != null && !authorization.isEmpty()) {
                                String authHeader = authorization.get(0);
                                token = authHeader;
                                
                                // Handle Bearer token format
                                if (authHeader.startsWith("Bearer ")) {
                                    token = authHeader.substring(7);
                                }
                                // Fix: Check token length before substring to avoid errors
                                if (token.length() > 10) {
                                    logger.info("Using token from STOMP headers: {}", token.substring(0, 10) + "...");
                                } else {
                                    logger.info("Using token from STOMP headers: {}", token);
                                }
                            }
                        }
                        
                        // Validate token if found
                        if (token != null) {
                            try {
                                User user = authService.getUserByToken(token);
                                
                                // Set authenticated user as Principal
                                accessor.setUser(new Principal() {
                                    @Override
                                    public String getName() {
                                        return user.getId().toString();
                                    }
                                });
                                
                                logger.info("STOMP authentication successful for user ID: {}", user.getId());
                            } catch (Exception e) {
                                logger.warn("STOMP authentication failed: {}", e.getMessage());
                                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
                            }
                        } 
                        // Use principal from handshake if already set
                        else if (accessor.getUser() == null) {
                            logger.warn("STOMP connection attempt without authorization");
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authorization provided");
                        }
                    } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        logger.info("STOMP SUBSCRIBE to: {}", accessor.getDestination());
                    } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        logger.info("STOMP SEND to: {}", accessor.getDestination());
                    } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                        logger.info("STOMP DISCONNECT from sessionId: {}", accessor.getSessionId());
                    }
                } catch (Exception e) {
                    logger.error("Error processing STOMP frame: {}", e.getMessage(), e);
                }
                
                return message;
            }
        });
        
        logger.info("Client inbound channel configured with authentication interceptor");
    }

    // Add outbound channel interceptor to debug CONNECTED responses
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null && accessor.getCommand() != null) {
                        logger.info("OUTBOUND STOMP frame: command={}, sessionId={}, destination={}", 
                            accessor.getCommand(), accessor.getSessionId(), accessor.getDestination());
                        
                        if (StompCommand.CONNECTED.equals(accessor.getCommand())) {
                            logger.info("Sending CONNECTED response to client");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error in outbound channel: {}", e.getMessage());
                }
                return message;
            }
        });
        
        logger.info("Client outbound channel configured with logging interceptor");
    }
}
