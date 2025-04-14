package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebSocketService
 * This class is responsible for sending messages to clients over WebSocket connections.
 * It uses the SimpMessagingTemplate to send messages to specific destinations.
 */
@Service
@Transactional
public class WebSocketService {

    private final Logger log = LoggerFactory.getLogger(WebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send a message to a specific user
     *
     * @param userId - the user ID
     * @param destination - the destination path
     * @param payload - the message payload
     */
    public void sendToUser(Long userId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                destination,
                payload
        );
        log.debug("Message sent to user {}: {}", userId, payload);
    }

    /**
     * Send a message to all users in a game
     *
     * @param gameId - the game ID
     * @param destination - the destination path
     * @param payload - the message payload
     */
    public void sendToGame(Long gameId, String destination, Object payload) {
        messagingTemplate.convertAndSend(
                destination,
                payload
        );
        log.debug("Message sent to game {}: {}", gameId, payload);
    }

    /**
     * Send a message to all connected users
     *
     * @param destination - the destination path
     * @param payload - the message payload
     */
    public void sendToAll(String destination, Object payload) {
        messagingTemplate.convertAndSend(
                destination,
                payload
        );
        log.debug("Message sent to all: {}", payload);
    }
}