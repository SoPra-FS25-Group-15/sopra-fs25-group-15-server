package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyManagementDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyManagerWebSocketControllerTest {

    @Mock
    private LobbyService lobbyService;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private DTOMapper mapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private WebSocketEventListener webSocketEventListener;

    @Mock
    private Principal principal;

    @Mock
    private StompHeaderAccessor headerAccessor;

    @InjectMocks
    private LobbyManagerWebSocketController lobbyManagerWebSocketController;

    private User testUser;
    private User testUser2;
    private Lobby testLobby;
    private String testToken = "test-token";
    private UserPublicDTO userPublicDTO;
    private LobbyManagementDTO.PendingInvite pendingInvite;
    private List<LobbyManagementDTO.PendingInvite> pendingInvites;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(testToken);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        testUser.setProfile(profile);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setToken("test-token-2");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("testUser2");
        testUser2.setProfile(profile2);

        testLobby = new Lobby();
        testLobby.setId(1L);
        testLobby.setLobbyCode("TEST123");
        testLobby.setHost(testUser);
        testLobby.setMaxPlayers(4);
        testLobby.setMaxPlayersPerTeam(2);
        testLobby.setPrivate(false);
        testLobby.setStatus("WAITING");
        testLobby.setMode("CLASSIC");
        List<String> hintsEnabled = new ArrayList<>();
        hintsEnabled.add("HINT1");
        testLobby.setHintsEnabled(hintsEnabled);
        List<User> players = new ArrayList<>();
        players.add(testUser);
        testLobby.setPlayers(players);

        userPublicDTO = new UserPublicDTO();
        userPublicDTO.setUserid(1L);
        userPublicDTO.setUsername("testUser");
        userPublicDTO.setXp(1000);
        userPublicDTO.setPoints(500);

        pendingInvite = new LobbyManagementDTO.PendingInvite();
        pendingInvite.setUsername("testUser2");
        pendingInvite.setLobbyCode("TEST123");
        pendingInvites = Collections.singletonList(pendingInvite);

        when(principal.getName()).thenReturn(testToken);

        when(authService.getUserByToken(testToken)).thenReturn(testUser);

        when(userService.getPublicProfile(1L)).thenReturn(testUser);
        when(userService.findByUsername("testUser2")).thenReturn(testUser2);
        when(userService.getPublicProfile(2L)).thenReturn(testUser2);

        when(lobbyService.getCurrentLobbyForUser(1L)).thenReturn(testLobby);
        when(lobbyService.getLobbyByCode("TEST123")).thenReturn(testLobby);

        List<LobbyService.LobbyInvite> invites = new ArrayList<>();
        LobbyService.LobbyInvite invite = new LobbyService.LobbyInvite();
        invite.setSender(testUser);
        invite.setLobbyCode("TEST123");
        invites.add(invite);
        when(lobbyService.getPendingInvitesForUser(anyLong())).thenReturn(invites);

        when(mapper.toUserPublicDTO(testUser)).thenReturn(userPublicDTO);

        when(headerAccessor.getSessionId()).thenReturn("test-session-id");
    }

    @Test
    void joinLobbyByCode_success_sendsSuccessMessage() {
        LobbyResponseDTO lobbyResponseDTO = new LobbyResponseDTO();
        lobbyResponseDTO.setLobbyId(1L);
        lobbyResponseDTO.setCode("TEST123");
        lobbyResponseDTO.setMode("CLASSIC");
        lobbyResponseDTO.setStatus("WAITING");

        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO();
        joinResponse.setMessage("Successfully joined the lobby");
        joinResponse.setLobby(lobbyResponseDTO);

        when(lobbyService.joinLobby(eq(1L), eq(1L), isNull(), eq("TEST123"), eq(false)))
                .thenReturn(joinResponse);

        WebSocketMessage<Void> message = new WebSocketMessage<>();

        lobbyManagerWebSocketController.joinLobbyByCode("TEST123", message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/join/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "JOIN_SUCCESS".equals(wsMsg.getType()) &&
                            joinResponse.equals(wsMsg.getPayload());
                })
        );
    }

    @Test
    void joinLobbyByCode_responseStatusException_sendsErrorMessage() {
        when(lobbyService.joinLobby(anyLong(), anyLong(), isNull(), anyString(), anyBoolean()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test error"));

        WebSocketMessage<Void> message = new WebSocketMessage<>();

        lobbyManagerWebSocketController.joinLobbyByCode("TEST123", message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/join/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "JOIN_ERROR".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("code").equals(400) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("Test error");
                })
        );
    }

    @Test
    void joinLobbyByCode_runtimeException_sendsErrorMessage() {
        when(lobbyService.joinLobby(anyLong(), anyLong(), isNull(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Unexpected error"));

        WebSocketMessage<Void> message = new WebSocketMessage<>();

        lobbyManagerWebSocketController.joinLobbyByCode("TEST123", message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/join/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "JOIN_ERROR".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("code").equals(500) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("An unexpected error occurred");
                })
        );
    }

    @Test
    void getLobbyManagementState_success_returnsState() {
        List<LobbyService.LobbyInvite> pendingInvites = new ArrayList<>();
        LobbyService.LobbyInvite invite = new LobbyService.LobbyInvite();
        invite.setSender(testUser);
        invite.setLobbyCode("TEST123");
        pendingInvites.add(invite);

        when(lobbyService.getPendingInvitesForUser(1L)).thenReturn(pendingInvites);

        WebSocketMessage<LobbyManagementDTO> result = lobbyManagerWebSocketController.getLobbyManagementState(principal);

        assertEquals("LOBBY_MANAGEMENT_STATE", result.getType());
        assertNotNull(result.getPayload());
        assertEquals("TEST123", result.getPayload().getCurrentLobbyCode());
        assertEquals(1, result.getPayload().getPendingInvites().size());
        assertEquals("testUser", result.getPayload().getPendingInvites().get(0).getUsername());
        assertEquals("TEST123", result.getPayload().getPendingInvites().get(0).getLobbyCode());
    }

    @Test
    void getLobbyManagementState_exception_returnsError() {
        when(lobbyService.getCurrentLobbyForUser(1L)).thenThrow(new RuntimeException("Test exception"));

        WebSocketMessage<LobbyManagementDTO> result = lobbyManagerWebSocketController.getLobbyManagementState(principal);

        assertEquals("LOBBY_MANAGEMENT_ERROR", result.getType());
        assertNull(result.getPayload());
    }

    @Test
    void sendLobbyInvite_success_sendsInviteMessages() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "testUser2");
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(payload);

        lobbyManagerWebSocketController.sendLobbyInvite(message, principal);

        verify(lobbyService).createLobbyInvite(testUser, testUser2, "TEST123");
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/invite/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_SENT".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("recipient").equals("testUser2") &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("lobbyCode").equals("TEST123");
                })
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/topic/lobby-manager/invites"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_IN".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("fromUsername").equals("testUser") &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("lobbyCode").equals("TEST123");
                })
        );
    }

    @Test
    void sendLobbyInvite_userNotFound_sendsErrorMessage() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "nonExistentUser");
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(payload);

        when(userService.findByUsername("nonExistentUser")).thenReturn(null);

        lobbyManagerWebSocketController.sendLobbyInvite(message, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/invite/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_ERROR".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("code").equals(404) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("User not found");
                })
        );
    }

    @Test
    void cancelLobbyInvite_success_sendsCancelMessages() {
        Map<String, String> payload = new HashMap<>();
        payload.put("toUsername", "testUser2");
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(payload);

        when(lobbyService.cancelLobbyInvite(testUser, testUser2)).thenReturn(true);

        lobbyManagerWebSocketController.cancelLobbyInvite(message, principal);

        verify(lobbyService).cancelLobbyInvite(testUser, testUser2);
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/invite/cancel/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_CANCELED".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("recipient").equals("testUser2");
                })
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("2"),
                eq("/topic/lobby-manager/invites"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_CANCELED".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("fromUsername").equals("testUser");
                })
        );
    }

    @Test
    void declineFriendLobbyInvite_success_sendsDeclineMessages() {
        Map<String, String> payload = new HashMap<>();
        payload.put("fromUsername", "testUser");
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(payload);

        when(userService.findByUsername("testUser")).thenReturn(testUser);
        when(lobbyService.hasAnyPendingInviteFrom(testUser, testUser2)).thenReturn(true);
        when(authService.getUserByToken(testToken)).thenReturn(testUser2);

        lobbyManagerWebSocketController.declineFriendLobbyInvite(message, principal, headerAccessor);

        verify(lobbyService).cancelLobbyInvite(testUser, testUser2);
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/invite/decline/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_DECLINED".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("fromUsername").equals("testUser") &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("Invite declined successfully");
                })
        );
        verify(messagingTemplate).convertAndSendToUser(
                eq("1"),
                eq("/topic/lobby-manager/invites/status"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_DECLINED".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("username").equals("testUser2") &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("Your invite was declined");
                })
        );
    }

    @Test
    void declineFriendLobbyInvite_invalidSession_throwsException() {
        Map<String, String> payload = new HashMap<>();
        payload.put("fromUsername", "testUser");
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(payload);

        when(headerAccessor.getSessionId()).thenReturn(null);

        lobbyManagerWebSocketController.declineFriendLobbyInvite(message, principal, headerAccessor);

        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/invite/decline/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "INVITE_DECLINE_ERROR".equals(wsMsg.getType()) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("code").equals(401) &&
                            ((Map<?, ?>)wsMsg.getPayload()).get("message").equals("Invalid session");
                })
        );
    }
}