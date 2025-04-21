package ch.uzh.ifi.hase.soprafs24.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    @Autowired
    private LobbyService lobbyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthService authService;
    
    // Track which WebSocket sessions belong to which lobbies
    private final Map<String, Long> sessionToLobbyMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            String principalName = principal.getName();
            logger.info("User connected: Session ID {} - User ID {}", 
                    headerAccessor.getSessionId(), principalName);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();
        
        if (principal == null) {
            logger.warn("User disconnected but no principal found: Session ID {}", sessionId);
            return;
        }
        
        String principalName = principal.getName();
        logger.info("User disconnected: Session ID {} - User ID {}", sessionId, principalName);
        
        // Check if this session was associated with a lobby
        Long lobbyId = sessionToLobbyMap.remove(sessionId);
        if (lobbyId != null) {
            logger.info("Session {} was associated with lobby {}", sessionId, lobbyId);
        }
        
        try {
            Long userId = null;
            
            // Try to determine if it's a numeric ID or a token
            if (principalName != null && principalName.matches("\\d+")) {
                // It's a numeric ID, parse directly
                userId = Long.parseLong(principalName);
            } else {
                // It's likely a token
                String token = TokenUtils.extractToken(principalName);
                
                try {
                    // Convert token to user ID
                    User user = authService.getUserByToken(token);
                    if (user != null) {
                        userId = user.getId();
                    }
                } catch (Exception e) {
                    logger.warn("Error looking up user by token: {}", e.getMessage());
                }
            }
            
            // If we found a user ID, handle their disconnection
            if (userId != null) {
                // Notify the lobby service that a user has disconnected
                lobbyService.handleUserDisconnect(userId);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket disconnect: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Register a session with a lobby to track which sessions are in which lobbies
     */
    public void registerSessionWithLobby(String sessionId, Long lobbyId) {
        if (sessionId != null && lobbyId != null) {
            sessionToLobbyMap.put(sessionId, lobbyId);
            logger.debug("Registered session {} with lobby {}", sessionId, lobbyId);
        }
    }
    
    /**
     * Unregister a session from its lobby
     */
    public void unregisterSessionFromLobby(String sessionId) {
        if (sessionId != null) {
            Long lobbyId = sessionToLobbyMap.remove(sessionId);
            if (lobbyId != null) {
                logger.debug("Unregistered session {} from lobby {}", sessionId, lobbyId);
            }
        }
    }
    
    /**
     * Get the lobby ID associated with a session
     */
    public Long getLobbyIdForSession(String sessionId) {
        return sessionToLobbyMap.get(sessionId);
    }
}
