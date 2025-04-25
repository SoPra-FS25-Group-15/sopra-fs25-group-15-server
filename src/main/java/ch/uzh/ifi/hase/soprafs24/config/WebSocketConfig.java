package ch.uzh.ifi.hase.soprafs24.config;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
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

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketErrorHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private WebSocketErrorHandler webSocketErrorHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[] {10000, 10000})
              .setTaskScheduler(new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler() {{
                  setPoolSize(1);
                  setThreadNamePrefix("websocket-heartbeat-thread-");
                  initialize();
              }});

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

        logger.info("Message broker configured with /topic, /app prefixes and 10s heartbeat");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        HandshakeInterceptor authInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {

                logger.info("WebSocket handshake request from: {}", request.getRemoteAddress());
                
                // Allow OPTIONS requests for CORS preflight
                if (request instanceof ServletServerHttpRequest && 
                    ((ServletServerHttpRequest) request).getServletRequest().getMethod().equals("OPTIONS")) {
                    return true;
                }

                // 1) Try to parse 'token=' from query params (works with SockJS fallback)
                String tokenFromQuery = getTokenFromQuery(request.getURI());
                if (tokenFromQuery != null) {
                    try {
                        User user = authService.getUserByToken(tokenFromQuery);
                        attributes.put("userId", user.getId());
                        attributes.put("username", user.getProfile().getUsername());
                        attributes.put("token", tokenFromQuery);
                        logger.info("WebSocket handshake authorized for user (query token): {}", user.getId());
                        return true;
                    } 
                    catch (Exception e) {
                        logger.warn("Handshake rejected (query param token invalid): {}", e.getMessage());
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return false;
                    }
                }

                // 2) If no token in query, fall back to the Authorization header approach
                List<String> authHeaders = request.getHeaders().get("Authorization");
                if (authHeaders == null || authHeaders.isEmpty()) {
                    logger.warn("WebSocket handshake rejected: No token in query and no Authorization header");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }

                String authHeader = authHeaders.get(0);
                String token = TokenUtils.extractToken(authHeader);

                try {
                    User user = authService.getUserByToken(token);
                    attributes.put("userId", user.getId());
                    attributes.put("username", user.getProfile().getUsername());
                    attributes.put("token", token);

                    logger.info("WebSocket handshake authorized for user (header token): {}", user.getId());
                    return true;
                } 
                catch (Exception e) {
                    logger.warn("Handshake rejected (Authorization header invalid): {}", e.getMessage());
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return false;
                }
            }

            @Override
            public void afterHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Exception exception) {

                if (exception != null) {
                    logger.error("Error during WebSocket handshake: {}", exception.getMessage());
                }
            }
        };

        // Custom handshake handler to set Principal
        DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(
                    ServerHttpRequest request,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes) {

                // If token is available in attributes, use that as the principal
                if (attributes.containsKey("token")) {
                    final String token = (String) attributes.get("token");
                    logger.info("Creating principal with token: " + 
                        (token.length() > 10 ? token.substring(0, 10) + "..." : token));
                    return () -> token;
                }
                // Fall back to user ID if token is not available
                else if (attributes.containsKey("userId")) {
                    final Long userId;
                    Object userIdObj = attributes.get("userId");

                    if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }

                    logger.info("Creating principal with user ID: " + userId);
                    return () -> userId.toString();
                }
                
                logger.warn("No token or userId in attributes for principal creation");
                return null;
            }
        };

        logger.info("Registering STOMP endpoints /ws/lobby, /ws/lobby-manager, /ws/user, /ws/friend, /ws/game");

        // SockJS endpoints with fallback (query param token + no cookies)
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game")
                .setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app") // Change to use patterns instead of origins
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .withSockJS()
                  .setSessionCookieNeeded(false)
                  .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                  .setWebSocketEnabled(true)
                  .setDisconnectDelay(5000);

        // // Raw WebSocket endpoints (no fallback). 
        // registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game")
        //         .setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app") // Change to use patterns instead of origins
        //         .addInterceptors(authInterceptor)
        //         .setHandshakeHandler(handshakeHandler);

        // Set the error handler for all endpoints
        registry.setErrorHandler(webSocketErrorHandler);

        logger.info("STOMP endpoints registered successfully");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor == null) {
                        logger.error("Received message with null accessor");
                        return message;
                    }
                    
                    if (accessor.getCommand() == null) {
                        // This might be a non-STOMP message (like heartbeat), just pass it through
                        return message;
                    }

                    logger.debug("STOMP command received: {}, sessionId={}",
                                accessor.getCommand(), accessor.getSessionId());
                    
                    if (accessor.getMessageHeaders() != null) {
                        accessor.getMessageHeaders().forEach((key, value) -> {
                            logger.trace("Header: {} = {}", key, value);
                        });
                    }

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        logger.info("Processing STOMP CONNECT frame");

                        // Attempt to get token from handshake attributes
                        String tokenValue = null;
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
                            tokenValue = (String) sessionAttributes.get("token");
                            if (tokenValue != null) {
                                if (tokenValue.length() > 10) {
                                    logger.info("Using token from handshake: {}...", tokenValue.substring(0, 10));
                                } else {
                                    logger.info("Using token from handshake: {}", tokenValue);
                                }
                            }
                        }

                        // Fallback to STOMP headers if needed
                        if (tokenValue == null) {
                            List<String> authorization = accessor.getNativeHeader("Authorization");
                            if (authorization != null && !authorization.isEmpty()) {
                                String authHeader = authorization.get(0);
                                tokenValue = TokenUtils.extractToken(authHeader);
                                
                                if (tokenValue != null && tokenValue.length() > 10) {
                                    logger.info("Using token from STOMP headers: {}...", tokenValue.substring(0, 10));
                                } else if (tokenValue != null) {
                                    logger.info("Using token from STOMP headers: {}", tokenValue);
                                }
                            }
                        }

                        // Validate token if found
                        if (tokenValue != null) {
                            try {
                                final String finalToken = tokenValue; // Create a final variable for use in lambda
                                User user = authService.getUserByToken(finalToken);
                                accessor.setUser(() -> finalToken); // Use the final variable
                                logger.info("Authentication successful for STOMP CONNECT - user: {}", user.getId());
                            } catch (Exception e) {
                                logger.error("Authentication failed for STOMP CONNECT: {}", e.getMessage());
                                throw e; // Let the exception propagate
                            }
                        } else {
                            logger.error("No valid token found for STOMP CONNECT");
                        }
                    }
                    else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        // Handle subscriptions - verify token here if needed
                        logger.debug("STOMP SUBSCRIBE to: {}", accessor.getDestination());
                    }
                } catch (Exception e) {
                    logger.error("Error processing STOMP frame: {}", e.getMessage(), e);
                }
                return message;
            }
        });

        logger.info("Client inbound channel configured with authentication interceptor");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    
                    if (accessor != null && accessor.getCommand() != null) {
                        logger.trace("Outbound STOMP: {} to {}", 
                            accessor.getCommand(), accessor.getDestination());
                    }
                } catch (Exception e) {
                    logger.warn("Error in outbound channel interceptor: {}", e.getMessage());
                }
                return message;
            }
        });

        logger.info("Client outbound channel configured with logging interceptor");
    }

    /**
     * Helper method to parse "token=" from query parameters, e.g. ?token=ABC123
     */
    private String getTokenFromQuery(URI uri) {
        String query = uri.getQuery();
        if (query == null || query.isEmpty()) {
            return null;
        }
        
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && "token".equals(parts[0])) {
                return parts[1];
            }
        }
        
        return null;
    }
}
