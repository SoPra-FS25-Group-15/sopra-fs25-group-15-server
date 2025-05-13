package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private Principal principal;

    @InjectMocks
    private WebSocketEventListener webSocketEventListener;

    private static final String SESSION_ID = "test-session-id";
    private static final String USER_ID = "123";
    private static final String TOKEN = "test-token";
    private static final Long LOBBY_ID = 456L;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testHandleWebSocketConnectListener_WithPrincipal() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(USER_ID);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionConnectedEvent event = new SessionConnectedEvent(this, message);

        webSocketEventListener.handleWebSocketConnectListener(event);

        verify(principal).getName();
    }

    @Test
    void testHandleWebSocketConnectListener_WithoutPrincipal() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        headerAccessor.setSessionId(SESSION_ID);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionConnectedEvent event = new SessionConnectedEvent(this, message);

        webSocketEventListener.handleWebSocketConnectListener(event);

        verify(principal, never()).getName();
    }

    @Test
    void testHandleWebSocketDisconnectListener_NoPrincipal() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setSessionId(SESSION_ID);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(lobbyService, never()).handleUserDisconnect(anyLong());
    }

    @Test
    void testHandleWebSocketDisconnectListener_WithNumericUserId() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(USER_ID);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(lobbyService).handleUserDisconnect(Long.parseLong(USER_ID));
    }

    @Test
    void testHandleWebSocketDisconnectListener_WithToken() throws Exception {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(TOKEN);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        User user = new User();
        user.setId(Long.parseLong(USER_ID));

        when(authService.getUserByToken(anyString())).thenReturn(user);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(authService).getUserByToken(tokenCaptor.capture());
        verify(lobbyService).handleUserDisconnect(Long.parseLong(USER_ID));

        assertTrue(tokenCaptor.getValue() != null);
    }

    @Test
    void testHandleWebSocketDisconnectListener_WithLobbyRegistration() {
        webSocketEventListener.registerSessionWithLobby(SESSION_ID, LOBBY_ID);

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(USER_ID);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        assertNull(webSocketEventListener.getLobbyIdForSession(SESSION_ID));
        verify(lobbyService).handleUserDisconnect(Long.parseLong(USER_ID));
    }

    @Test
    void testHandleWebSocketDisconnectListener_TokenLookupFailure() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(TOKEN);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        when(authService.getUserByToken(anyString())).thenThrow(new RuntimeException("Token not found"));

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(authService).getUserByToken(anyString());
        verify(lobbyService, never()).handleUserDisconnect(anyLong());
    }

    @Test
    void testHandleWebSocketDisconnectListener_UserNotFound() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(TOKEN);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        when(authService.getUserByToken(anyString())).thenReturn(null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(authService).getUserByToken(anyString());
        verify(lobbyService, never()).handleUserDisconnect(anyLong());
    }

    @Test
    void testRegisterSessionWithLobby() {
        webSocketEventListener.registerSessionWithLobby(SESSION_ID, LOBBY_ID);

        assertEquals(LOBBY_ID, webSocketEventListener.getLobbyIdForSession(SESSION_ID));
    }

    @Test
    void testRegisterSessionWithLobby_NullLobbyId() {
        webSocketEventListener.registerSessionWithLobby(SESSION_ID, null);

        assertNull(webSocketEventListener.getLobbyIdForSession(SESSION_ID));
    }

    @Test
    void testUnregisterSessionFromLobby() {
        webSocketEventListener.registerSessionWithLobby(SESSION_ID, LOBBY_ID);

        webSocketEventListener.unregisterSessionFromLobby(SESSION_ID);

        assertNull(webSocketEventListener.getLobbyIdForSession(SESSION_ID));
    }

    @Test
    void testUnregisterSessionFromLobby_NullSessionId() {
        webSocketEventListener.unregisterSessionFromLobby(null);

        assertTrue(true);
    }

    @Test
    void testUnregisterSessionFromLobby_NonExistentSession() {
        webSocketEventListener.unregisterSessionFromLobby("non-existent-session");

        assertNull(webSocketEventListener.getLobbyIdForSession("non-existent-session"));
    }

    @Test
    void testGetLobbyIdForSession_NotRegistered() {
        Long result = webSocketEventListener.getLobbyIdForSession("unknown-session");

        assertNull(result);
    }

    @Test
    void testHandleWebSocketDisconnectListener_PrincipalNameNull() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn(null);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(lobbyService, never()).handleUserDisconnect(anyLong());
    }

    @Test
    void testHandleWebSocketDisconnectListener_InvalidNumericUserId() {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        headerAccessor.setUser(principal);
        headerAccessor.setSessionId(SESSION_ID);
        when(principal.getName()).thenReturn("not_a_number");

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(headerAccessor)
                .build();

        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, SESSION_ID, null);

        when(authService.getUserByToken(anyString())).thenReturn(null);

        webSocketEventListener.handleWebSocketDisconnectListener(event);

        verify(lobbyService, never()).handleUserDisconnect(anyLong());
    }
}