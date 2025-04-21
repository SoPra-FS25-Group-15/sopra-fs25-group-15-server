package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils; // Fix: Add TokenUtils import
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;

@Controller
public class FriendWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    // Helper method to validate that the user is authenticated.
    private String validateAuthentication(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        String principalName = principal.getName();
        
        // Try to handle both token and numeric ID formats
        if (principalName != null && principalName.matches("\\d+")) {
            try {
                // If it's a user ID, find the associated token
                User user = userService.getPublicProfile(Long.parseLong(principalName));
                if (user != null && user.getToken() != null) {
                    return user.getToken();
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
            }
        }
        
        // Extract token if it starts with Bearer
        String token = TokenUtils.extractToken(principalName);
        
        try {
            User user = authService.getUserByToken(token);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
            }
            return token;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
        }
    }

    /**
     * 1. Send friend request (REQUEST_OUT)
     *    Payload example:
     *    { type: "REQUEST_OUT", token: "...", toUsername: "alice" }
     */
    @MessageMapping("/friend/requestOut")
    public void sendFriendRequest(@Payload WebSocketMessage<Map<String, String>> message, Principal principal) {
        String userToken = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        
        // Get user by token
        User user = authService.getUserByToken(userToken);

        // Lookup the recipient by username.
        User recipient = userService.findByUsername(toUsername);
        if (recipient == null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestOut/result", 
                new WebSocketMessage<>("REQUEST_ERROR", "User '" + toUsername + "' not found"));
            return;
        }
        try {
            friendService.sendFriendRequest(userToken, recipient.getId());
            // Notify the sender that the friend request was sent successfully.
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestOut/result", 
                new WebSocketMessage<>("REQUEST_SENT", "Friend request sent to " + toUsername));
            
            // Retrieve sender's username to include in the notification.
            String senderUsername = user.getProfile().getUsername(); // Fix: Use getProfile().getUsername()
            // Notify the recipient that an incoming friend request has been received.
            messagingTemplate.convertAndSendToUser(recipient.getId().toString(), "/topic/friend/incoming", 
                new WebSocketMessage<>("REQUEST_IN", Map.of("fromUsername", senderUsername)));
        } catch (ResponseStatusException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestOut/result", 
                new WebSocketMessage<>("REQUEST_ERROR", e.getReason()));
        }
    }

    /**
     * 2. Respond to a received friend request (REQUEST_RESPONSE)
     *    Payload example:
     *    { type: "REQUEST_RESPONSE", token: "...", toUsername: "bob", accept: "true" }
     *
     *    If accepted, the original sender will receive a "FRIEND_ACCEPTED" message.
     */
    @MessageMapping("/friend/requestResponse")
    public void respondToFriendRequest(@Payload WebSocketMessage<Map<String, String>> message, Principal principal) {
        String userToken = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        // 'accept' is passed as a string ("true" or "false")
        boolean accept = Boolean.parseBoolean(payload.get("accept"));
        
        // Get user by token
        User responder = authService.getUserByToken(userToken);
        
        // Lookup the original sender by username.
        User requester = userService.findByUsername(toUsername);
        if (requester == null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestResponse/result",
                new WebSocketMessage<>("RESPONSE_ERROR", "User '" + toUsername + "' not found"));
            return;
        }
        try {
            friendService.respondToFriendRequestBySender(userToken, requester.getId(), accept ? "accept" : "deny");

            // If the friend request was accepted, notify the original sender.
            if (accept) {
                String responderUsername = responder.getProfile().getUsername(); // Fix: Use getProfile().getUsername()
                messagingTemplate.convertAndSendToUser(requester.getId().toString(), "/topic/friend/requestResponse", 
                    new WebSocketMessage<>("FRIEND_ACCEPTED", Map.of("fromUsername", responderUsername)));
            }
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestResponse/result",
                new WebSocketMessage<>("REQUEST_RESPONSE_PROCESSED", "Response sent to " + toUsername));
        } catch (ResponseStatusException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/requestResponse/result",
                new WebSocketMessage<>("RESPONSE_ERROR", e.getReason()));
        }
    }

    /**
     * 3. Cancel a friend request you sent (CANCEL_REQUEST)
     *    Payload example:
     *    { type: "CANCEL_REQUEST", token: "...", toUsername: "charlie" }
     */
    @MessageMapping("/friend/cancelRequest")
    public void cancelFriendRequest(@Payload WebSocketMessage<Map<String, String>> message, Principal principal) {
        String userToken = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        
        // Get user by token
        User sender = authService.getUserByToken(userToken);
        // Lookup the recipient by username.
        User recipient = userService.findByUsername(toUsername);
        if (recipient == null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/cancelRequest/result",
                new WebSocketMessage<>("CANCEL_ERROR", "User '" + toUsername + "' not found"));
            return;
        }
        try {
            friendService.cancelFriendRequestToUser(userToken, recipient.getId());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/cancelRequest/result",
                new WebSocketMessage<>("CANCEL_REQUEST", "Friend request canceled"));
        } catch (ResponseStatusException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/cancelRequest/result",
                new WebSocketMessage<>("CANCEL_ERROR", e.getReason()));
        }
    }

    /**
     * 4. Remove a friend (REMOVE_FRIEND)
     *    Payload example:
     *    { type: "REMOVE_FRIEND", token: "...", toUsername: "dave" }
     */
    @MessageMapping("/friend/removeFriend")
    public void removeFriend(@Payload WebSocketMessage<Map<String, String>> message, Principal principal) {
        String userToken = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        
        // Get user by token
        User user = authService.getUserByToken(userToken);
        // Lookup the friend by username.
        User friend = userService.findByUsername(toUsername);
        if (friend == null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/removeFriend/result",
                new WebSocketMessage<>("REMOVE_ERROR", "User '" + toUsername + "' not found"));
            return;
        }
        try {
            friendService.unfriend(userToken, friend.getId());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/removeFriend/result",
                new WebSocketMessage<>("REMOVE_FRIEND", "Friend removed successfully"));
        } catch (ResponseStatusException e) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/topic/friend/removeFriend/result",
                new WebSocketMessage<>("REMOVE_ERROR", e.getReason()));
        }
    }
}
