package ch.uzh.ifi.hase.soprafs24.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private AuthService authService;

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
                String token = authHeader;

                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }

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

                if (attributes.containsKey("userId")) {
                    final Long userId;
                    Object userIdObj = attributes.get("userId");

                    if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }

                    logger.info("Creating principal for user: " + userId);
                    return () -> userId.toString();
                }
                return null;
            }
        };

        logger.info("Registering STOMP endpoints /ws/lobby, /ws/lobby-manager, /ws/user, /ws/friend");

        // SockJS endpoints with fallback (query param token + no cookies)
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend")
                .setAllowedOrigins("http://localhost:3000", "https://sopra-fs25-group-15-server.oa.r.appspot.com/")
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(handshakeHandler)
                .withSockJS()
                  .setSessionCookieNeeded(false)
                  .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                  .setWebSocketEnabled(true)
                  .setDisconnectDelay(5000);

        // Raw WebSocket endpoints (no fallback). 
        registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend")
                .setAllowedOrigins("http://localhost:3000", "https://sopra-fs25-group-15-server.oa.r.appspot.com/")
                .addInterceptors(authInterceptor)
                .setHandshakeHandler(handshakeHandler);

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
                    if (accessor == null || accessor.getCommand() == null) {
                        logger.error("Received malformed STOMP frame or non-STOMP message");
                        return message;
                    }

                    logger.info("STOMP command received: {}, sessionId={}, payload={}",
                                accessor.getCommand(), accessor.getSessionId(),
                                message.getPayload() != null
                                    ? message.getPayload().toString()
                                    : "null");

                    accessor.toNativeHeaderMap().forEach((key, value) -> {
                        logger.info("STOMP header: {} = {}", key, value);
                    });

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        logger.info("Processing STOMP CONNECT frame");

                        // Attempt to get token from handshake attributes
                        String token = null;
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
                            token = (String) sessionAttributes.get("token");
                            if (token.length() > 10) {
                                logger.info("Using token from handshake: {}...", token.substring(0, 10));
                            } else {
                                logger.info("Using token from handshake: {}", token);
                            }
                        }

                        // Fallback to STOMP headers if needed
                        if (token == null) {
                            List<String> authorization = accessor.getNativeHeader("Authorization");
                            if (authorization != null && !authorization.isEmpty()) {
                                String authHeader = authorization.get(0);
                                token = authHeader;
                                if (authHeader.startsWith("Bearer ")) {
                                    token = authHeader.substring(7);
                                }
                                if (token.length() > 10) {
                                    logger.info("Using token from STOMP headers: {}...", token.substring(0, 10));
                                } else {
                                    logger.info("Using token from STOMP headers: {}", token);
                                }
                            }
                        }

                        // Validate token if found
                        if (token != null) {
                            try {
                                User user = authService.getUserByToken(token);
                                accessor.setUser(() -> user.getId().toString());
                                logger.info("STOMP authentication successful for user ID: {}", user.getId());
                            } catch (Exception e) {
                                logger.warn("STOMP authentication failed: {}", e.getMessage());
                                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
                            }
                        }
                        else if (accessor.getUser() == null) {
                            logger.warn("STOMP connection attempt without authorization");
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authorization provided");
                        }
                    }
                    else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        logger.info("STOMP SUBSCRIBE to: {}", accessor.getDestination());
                    }
                    else if (StompCommand.SEND.equals(accessor.getCommand())) {
                        logger.info("STOMP SEND to: {}", accessor.getDestination());
                    }
                    else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
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

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
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

    /**
     * Helper method to parse "token=" from query parameters, e.g. ?token=ABC123
     */
    private String getTokenFromQuery(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String query = uri.getQuery(); // e.g. "token=ABC123&foo=bar"
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String raw = param.substring("token=".length());
                // decode in case we have URL-encoded tokens
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
