package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FriendWebSocketControllerTest {

    @InjectMocks
    private FriendWebSocketController friendWebSocketController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private FriendService friendService;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private Principal principal;

    private User testUser;
    private User recipientUser;
    private final String TEST_TOKEN = "test-token";
    private final String TEST_USERNAME = "testUser";
    private final String RECIPIENT_USERNAME = "recipientUser";
    private final Long TEST_USER_ID = 1L;
    private final Long RECIPIENT_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setToken(TEST_TOKEN);
        UserProfile testUserProfile = new UserProfile();
        testUserProfile.setUsername(TEST_USERNAME);
        testUser.setProfile(testUserProfile);

        recipientUser = new User();
        recipientUser.setId(RECIPIENT_USER_ID);
        UserProfile recipientUserProfile = new UserProfile();
        recipientUserProfile.setUsername(RECIPIENT_USERNAME);
        recipientUser.setProfile(recipientUserProfile);

        when(principal.getName()).thenReturn(TEST_TOKEN);
        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(testUser);
        when(userService.findByUsername(RECIPIENT_USERNAME)).thenReturn(recipientUser);
    }

    @Test
    void sendFriendRequest_Success() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        friendWebSocketController.sendFriendRequest(message, principal);

        verify(friendService).sendFriendRequest(TEST_TOKEN, RECIPIENT_USER_ID);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestOut/result"),
                any(WebSocketMessage.class)
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq(RECIPIENT_USER_ID.toString()),
                eq("/topic/friend/incoming"),
                any(WebSocketMessage.class)
        );
    }

    @Test
    void sendFriendRequest_RecipientNotFound() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "nonExistentUser");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        when(userService.findByUsername("nonExistentUser")).thenReturn(null);

        friendWebSocketController.sendFriendRequest(message, principal);

        verify(friendService, never()).sendFriendRequest(anyString(), anyLong());

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestOut/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "REQUEST_ERROR".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void sendFriendRequest_ServiceException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already friends"))
                .when(friendService).sendFriendRequest(TEST_TOKEN, RECIPIENT_USER_ID);

        friendWebSocketController.sendFriendRequest(message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestOut/result"),
                argThat(msg -> msg instanceof WebSocketMessage &&
                        "REQUEST_ERROR".equals(((WebSocketMessage<?>) msg).getType()) &&
                        "Already friends".equals(((WebSocketMessage<?>) msg).getPayload()))
        );
    }

    @Test
    void respondToFriendRequest_AcceptSuccess() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);
        payload.put("accept", "true");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_RESPONSE");
        message.setPayload(payload);

        friendWebSocketController.respondToFriendRequest(message, principal);

        verify(friendService).respondToFriendRequestBySender(TEST_TOKEN, RECIPIENT_USER_ID, "accept");

        verify(messagingTemplate).convertAndSendToUser(
                eq(RECIPIENT_USER_ID.toString()),
                eq("/topic/friend/requestResponse"),
                any(WebSocketMessage.class)
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestResponse/result"),
                any(WebSocketMessage.class)
        );
    }

    @Test
    void respondToFriendRequest_DenySuccess() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);
        payload.put("accept", "false");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_RESPONSE");
        message.setPayload(payload);

        friendWebSocketController.respondToFriendRequest(message, principal);

        verify(friendService).respondToFriendRequestBySender(TEST_TOKEN, RECIPIENT_USER_ID, "deny");

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(RECIPIENT_USER_ID.toString()),
                eq("/topic/friend/requestResponse"),
                any(WebSocketMessage.class)
        );

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestResponse/result"),
                any(WebSocketMessage.class)
        );
    }

    @Test
    void respondToFriendRequest_RequesterNotFound() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "nonExistentUser");
        payload.put("accept", "true");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_RESPONSE");
        message.setPayload(payload);

        when(userService.findByUsername("nonExistentUser")).thenReturn(null);

        friendWebSocketController.respondToFriendRequest(message, principal);

        verify(friendService, never()).respondToFriendRequestBySender(anyString(), anyLong(), anyString());

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestResponse/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "RESPONSE_ERROR".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void respondToFriendRequest_ServiceException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);
        payload.put("accept", "true");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_RESPONSE");
        message.setPayload(payload);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending request"))
                .when(friendService).respondToFriendRequestBySender(TEST_TOKEN, RECIPIENT_USER_ID, "accept");

        friendWebSocketController.respondToFriendRequest(message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/requestResponse/result"),
                argThat(msg -> msg instanceof WebSocketMessage &&
                        "RESPONSE_ERROR".equals(((WebSocketMessage<?>) msg).getType()) &&
                        "No pending request".equals(((WebSocketMessage<?>) msg).getPayload()))
        );
    }

    @Test
    void cancelFriendRequest_Success() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("CANCEL_REQUEST");
        message.setPayload(payload);

        friendWebSocketController.cancelFriendRequest(message, principal);

        verify(friendService).cancelFriendRequestToUser(TEST_TOKEN, RECIPIENT_USER_ID);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/cancelRequest/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "CANCEL_REQUEST".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void cancelFriendRequest_RecipientNotFound() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "nonExistentUser");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("CANCEL_REQUEST");
        message.setPayload(payload);

        when(userService.findByUsername("nonExistentUser")).thenReturn(null);

        friendWebSocketController.cancelFriendRequest(message, principal);

        verify(friendService, never()).cancelFriendRequestToUser(anyString(), anyLong());

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/cancelRequest/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "CANCEL_ERROR".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void cancelFriendRequest_ServiceException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("CANCEL_REQUEST");
        message.setPayload(payload);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending request"))
                .when(friendService).cancelFriendRequestToUser(TEST_TOKEN, RECIPIENT_USER_ID);

        friendWebSocketController.cancelFriendRequest(message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/cancelRequest/result"),
                argThat(msg -> msg instanceof WebSocketMessage &&
                        "CANCEL_ERROR".equals(((WebSocketMessage<?>) msg).getType()) &&
                        "No pending request".equals(((WebSocketMessage<?>) msg).getPayload()))
        );
    }

    @Test
    void removeFriend_Success() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REMOVE_FRIEND");
        message.setPayload(payload);

        friendWebSocketController.removeFriend(message, principal);

        verify(friendService).unfriend(TEST_TOKEN, RECIPIENT_USER_ID);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/removeFriend/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "REMOVE_FRIEND".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void removeFriend_FriendNotFound() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "nonExistentUser");

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REMOVE_FRIEND");
        message.setPayload(payload);

        when(userService.findByUsername("nonExistentUser")).thenReturn(null);

        friendWebSocketController.removeFriend(message, principal);

        verify(friendService, never()).unfriend(anyString(), anyLong());

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/removeFriend/result"),
                argThat(msg -> msg instanceof WebSocketMessage && "REMOVE_ERROR".equals(((WebSocketMessage<?>) msg).getType()))
        );
    }

    @Test
    void removeFriend_ServiceException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REMOVE_FRIEND");
        message.setPayload(payload);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not friends"))
                .when(friendService).unfriend(TEST_TOKEN, RECIPIENT_USER_ID);

        friendWebSocketController.removeFriend(message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/topic/friend/removeFriend/result"),
                argThat(msg -> msg instanceof WebSocketMessage &&
                        "REMOVE_ERROR".equals(((WebSocketMessage<?>) msg).getType()) &&
                        "Not friends".equals(((WebSocketMessage<?>) msg).getPayload()))
        );
    }

    @Test
    void validateAuthentication_Success() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        friendWebSocketController.sendFriendRequest(message, principal);

        verify(authService, atLeastOnce()).getUserByToken(TEST_TOKEN);
    }

    @Test
    void validateAuthentication_PrincipalNull() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        Principal nullPrincipal = null;

        try {
            friendWebSocketController.sendFriendRequest(message, nullPrincipal);
        } catch (ResponseStatusException e) {
            assert e.getStatus() == HttpStatus.UNAUTHORIZED;
            assert "Not authenticated".equals(e.getReason());
        }

        verify(friendService, never()).sendFriendRequest(anyString(), anyLong());
    }

    @Test
    void validateAuthentication_InvalidToken() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(null);

        try {
            friendWebSocketController.sendFriendRequest(message, principal);
        } catch (ResponseStatusException e) {
            assert e.getStatus() == HttpStatus.UNAUTHORIZED;
            assert "Invalid authentication".equals(e.getReason());
        }

        verify(friendService, never()).sendFriendRequest(anyString(), anyLong());
    }

    @Test
    void validateAuthentication_TokenExtractionException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        when(authService.getUserByToken(anyString())).thenThrow(new RuntimeException("Token error"));

        try {
            friendWebSocketController.sendFriendRequest(message, principal);
        } catch (ResponseStatusException e) {
            assert e.getStatus() == HttpStatus.UNAUTHORIZED;
            assert "Invalid authentication".equals(e.getReason());
        }

        verify(friendService, never()).sendFriendRequest(anyString(), anyLong());
    }

    @Test
    void validateAuthentication_UserIdPrincipal() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", RECIPIENT_USERNAME);

        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setType("REQUEST_OUT");
        message.setPayload(payload);

        when(principal.getName()).thenReturn(TEST_USER_ID.toString());
        when(userService.getPublicProfile(TEST_USER_ID)).thenReturn(testUser);

        friendWebSocketController.sendFriendRequest(message, principal);

        verify(userService).getPublicProfile(TEST_USER_ID);
        verify(friendService).sendFriendRequest(TEST_TOKEN, RECIPIENT_USER_ID);
    }
}