package ch.uzh.ifi.hase.soprafs24.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;

@Component
public class WebSocketEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Session ID to Lobby ID mapping to track which lobby a user is in
    private final Map<String, Long> sessionLobbyMap = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser().getName();
        
        log.info("User connected: Session ID {} - User ID {}", sessionId, username);
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "Unknown";
        
        log.info("User disconnected: Session ID {} - User ID {}", sessionId, username);
        
        // Check if user was in a lobby
        Long lobbyId = sessionLobbyMap.remove(sessionId);
        if (lobbyId != null && headerAccessor.getUser() != null) {
            // Notify others in the lobby that user has disconnected
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId + "/users",
                new WebSocketMessage<>("USER_DISCONNECTED", Long.parseLong(username))
            );
            
            log.info("User {} disconnected from lobby {}", username, lobbyId);
        }
    }
    
    /**
     * Register a session with a lobby for tracking purposes
     */
    public void registerSessionWithLobby(String sessionId, Long lobbyId) {
        sessionLobbyMap.put(sessionId, lobbyId);
    }
    
    /**
     * Remove a session from a lobby tracking
     */
    public void unregisterSessionFromLobby(String sessionId) {
        sessionLobbyMap.remove(sessionId);
    }
}
