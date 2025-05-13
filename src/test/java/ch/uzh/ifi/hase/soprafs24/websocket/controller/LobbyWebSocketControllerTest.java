package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyDTO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyWebSocketControllerTest {

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
    private LobbyWebSocketController lobbyWebSocketController;

    private User testUser;
    private Lobby testLobby;
    private String testToken = "test-token";
    private UserPublicDTO userPublicDTO;
    private LobbyResponseDTO lobbyResponseDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(testToken);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        testUser.setProfile(profile);

        // Setup test lobby
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

        // Setup user DTO
        userPublicDTO = new UserPublicDTO();
        userPublicDTO.setUserid(1L);
        userPublicDTO.setUsername("testUser");

        // Setup lobby response DTO
        lobbyResponseDTO = new LobbyResponseDTO();
        lobbyResponseDTO.setLobbyId(1L);
        lobbyResponseDTO.setCode("TEST123");
        lobbyResponseDTO.setMaxPlayers("4");
        lobbyResponseDTO.setPlayersPerTeam(2);
        lobbyResponseDTO.setStatus("WAITING");
        lobbyResponseDTO.setMode("CLASSIC");
        lobbyResponseDTO.setPrivate(false);
        lobbyResponseDTO.setRoundCardsStartAmount(1);

        // Setup principal mock
        when(principal.getName()).thenReturn(testToken);

        // Setup auth service mock
        when(authService.getUserByToken(testToken)).thenReturn(testUser);

        // Setup user service mock
        when(userService.getPublicProfile(1L)).thenReturn(testUser);

        // Setup lobby service mock
        when(lobbyService.getLobbyByCode("TEST123")).thenReturn(testLobby);

        // Setup DTOMapper mock
        when(mapper.toUserPublicDTO(testUser)).thenReturn(userPublicDTO);
        when(mapper.lobbyEntityToResponseDTO(testLobby)).thenReturn(lobbyResponseDTO);

        // Setup StompHeaderAccessor mock
        when(headerAccessor.getSessionId()).thenReturn("test-session-id");
    }

//    @Test
//    void createLobby_success_sendsSuccessMessage() {
//        // Setup
//        WebSocketMessage<LobbyRequestDTO> message = new WebSocketMessage<>();
//        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
//        requestDTO.setMaxPlayers(4);
//        requestDTO.setPrivate(false);
//        message.setPayload(requestDTO);
//
//        when(mapper.lobbyRequestDTOToEntity(requestDTO)).thenReturn(testLobby);
//        when(lobbyService.createLobby(testLobby)).thenReturn(testLobby);
//
//        // Test
//        lobbyWebSocketController.createLobby(message, principal, headerAccessor);
//
//        // Verify
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(testToken),
//                eq("/topic/lobby/create/result"),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_CREATED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload() instanceof LobbyDTO &&
//                            "TEST123".equals(((LobbyDTO)wsMsg.getPayload()).getCode());
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobbies")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_ADDED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload() instanceof LobbyDTO &&
//                            "TEST123".equals(((LobbyDTO)wsMsg.getPayload()).getCode());
//                })
//        );
//        verify(webSocketEventListener).registerSessionWithLobby("test-session-id", 1L);
//    }

    @Test
    void createLobby_invalidSession_sendsErrorMessage() {
        // Setup
        WebSocketMessage<LobbyRequestDTO> message = new WebSocketMessage<>();
        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
        requestDTO.setMaxPlayers(4);
        requestDTO.setPrivate(false);
        message.setPayload(requestDTO);

        when(headerAccessor.getSessionId()).thenReturn(null);

        // Test
        lobbyWebSocketController.createLobby(message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/create/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "LOBBY_CREATE_ERROR".equals(wsMsg.getType()) &&
                            "Invalid session".equals(wsMsg.getPayload());
                })
        );
    }

    @Test
    void createLobby_exception_sendsErrorMessage() {
        // Setup
        WebSocketMessage<LobbyRequestDTO> message = new WebSocketMessage<>();
        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
        requestDTO.setMaxPlayers(4);
        requestDTO.setPrivate(false);
        message.setPayload(requestDTO);

        when(mapper.lobbyRequestDTOToEntity(requestDTO)).thenReturn(testLobby);
        when(lobbyService.createLobby(testLobby)).thenThrow(new RuntimeException("Test error"));

        // Test
        lobbyWebSocketController.createLobby(message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/create/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "LOBBY_CREATE_ERROR".equals(wsMsg.getType()) &&
                            "Test error".equals(wsMsg.getPayload());
                })
        );
    }

//    @Test
//    void updateLobbySettings_success_sendsUpdateMessage() {
//        // Setup
//        WebSocketMessage<LobbyDTO> message = new WebSocketMessage<>();
//        LobbyDTO lobbyDTO = new LobbyDTO();
//        lobbyDTO.setMaxPlayers("5");
//        lobbyDTO.setPlayersPerTeam(3);
//        message.setPayload(lobbyDTO);
//
//        when(lobbyService.updateLobbyConfig(eq(1L), any(LobbyConfigUpdateRequestDTO.class), eq(1L)))
//                .thenReturn(testLobby);
//
//        // Test
//        lobbyWebSocketController.updateLobbySettings(1L, message, principal, headerAccessor);
//
//        // Verify
//        verify(lobbyService).updateLobbyConfig(eq(1L), any(LobbyConfigUpdateRequestDTO.class), eq(1L));
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby/1")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "UPDATE_SUCCESS".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload() instanceof LobbyDTO;
//                })
//        );
//    }

    @Test
    void updateLobbySettings_invalidSession_sendsErrorMessage() {
        // Setup
        WebSocketMessage<LobbyDTO> message = new WebSocketMessage<>();
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setMaxPlayers("5");
        lobbyDTO.setPlayersPerTeam(3);
        message.setPayload(lobbyDTO);

        when(headerAccessor.getSessionId()).thenReturn(null);

        // Test
        lobbyWebSocketController.updateLobbySettings(1L, message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/update/error"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "UPDATE_ERROR".equals(wsMsg.getType()) &&
                            "Invalid session".equals(wsMsg.getPayload());
                })
        );
    }

    @Test
    void updateLobbySettings_invalidFormat_sendsErrorMessage() {
        // Setup
        WebSocketMessage<LobbyDTO> message = new WebSocketMessage<>();
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setMaxPlayers("invalid"); // Not a number
        message.setPayload(lobbyDTO);

        // Test
        lobbyWebSocketController.updateLobbySettings(1L, message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/update/error"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "UPDATE_ERROR".equals(wsMsg.getType()) &&
                            "Invalid parameters submitted.".equals(wsMsg.getPayload());
                })
        );
    }

    @Test
    void updateLobbySettings_notHost_sendsErrorMessage() {
        // Setup
        WebSocketMessage<LobbyDTO> message = new WebSocketMessage<>();
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setMaxPlayers("5");
        message.setPayload(lobbyDTO);

        when(lobbyService.updateLobbyConfig(eq(1L), any(LobbyConfigUpdateRequestDTO.class), eq(1L)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only host can update"));

        // Test
        lobbyWebSocketController.updateLobbySettings(1L, message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/update/error"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "UPDATE_ERROR".equals(wsMsg.getType()) &&
                            "Only the host can update lobby configuration".equals(wsMsg.getPayload());
                })
        );
    }
//
//    @Test
//    void leaveLobby_asHost_disbandLobby() {
//        // Setup
//        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
//        message.setPayload(new HashMap<>());
//
//        when(lobbyService.isUserHost(1L, 1L)).thenReturn(true);
//        GenericMessageResponseDTO responseDTO = new GenericMessageResponseDTO();
//        responseDTO.setMessage("Lobby disbanded");
//        when(lobbyService.deleteLobby(1L, 1L)).thenReturn(responseDTO);
//
//        // Test
//        lobbyWebSocketController.leaveLobby(1L, message, principal, headerAccessor);
//
//        // Verify
//        verify(webSocketEventListener).unregisterSessionFromLobby("test-session-id");
//        verify(lobbyService).deleteLobby(1L, 1L);
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(testToken),
//                eq("/topic/lobby/leave/result"),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LEAVE_SUCCESS".equals(wsMsg.getType());
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby/1")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_DISBANDED".equals(wsMsg.getType());
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby-manager/lobbies"),
//                Optional.ofNullable(argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_REMOVED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload().equals(1L);
//                }))
//        );
//    }
//
//    @Test
//    void leaveLobby_asPlayer_leavesLobby() {
//        // Setup
//        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
//        message.setPayload(new HashMap<>());
//
//        when(lobbyService.isUserHost(1L, 1L)).thenReturn(false);
//        LobbyLeaveResponseDTO responseDTO = new LobbyLeaveResponseDTO();
//        responseDTO.setMessage("User left successfully");
//        responseDTO.setLobby(lobbyResponseDTO);
//        when(lobbyService.leaveLobby(1L, 1L, 1L)).thenReturn(responseDTO);
//
//        // Test
//        lobbyWebSocketController.leaveLobby(1L, message, principal, headerAccessor);
//
//        // Verify
//        verify(webSocketEventListener).unregisterSessionFromLobby("test-session-id");
//        verify(lobbyService).leaveLobby(1L, 1L, 1L);
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(testToken),
//                eq("/topic/lobby/leave/result"),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LEAVE_SUCCESS".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload() instanceof LobbyDTO;
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby/1/users")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "USER_LEFT".equals(wsMsg.getType()) &&
//                            ((Map<?, ?>)wsMsg.getPayload()).get("userId").equals(1L);
//                })
//        );
//    }

    @Test
    void leaveLobby_error_sendsErrorMessage() {
        // Setup
        WebSocketMessage<Map<String, String>> message = new WebSocketMessage<>();
        message.setPayload(new HashMap<>());

        when(lobbyService.isUserHost(1L, 1L)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not in lobby"));

        // Test
        lobbyWebSocketController.leaveLobby(1L, message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/leave/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "LEAVE_ERROR".equals(wsMsg.getType()) &&
                            "User not in lobby".equals(wsMsg.getPayload());
                })
        );
    }
//
//    @Test
//    void deleteLobby_success_sendsDeletedMessage() {
//        // Setup
//        GenericMessageResponseDTO responseDTO = new GenericMessageResponseDTO();
//        responseDTO.setMessage("Lobby deleted successfully");
//        when(lobbyService.deleteLobby(1L, 1L)).thenReturn(responseDTO);
//
//        // Test
//        lobbyWebSocketController.deleteLobby(1L, principal, headerAccessor);
//
//        // Verify
//        verify(lobbyService).deleteLobby(1L, 1L);
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(testToken),
//                eq("/topic/lobby-manager/delete/result"),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_DELETED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload().equals(responseDTO);
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby-manager/lobbies")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_REMOVED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload().equals(1L);
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby/1")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "LOBBY_DISBANDED".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload().equals(responseDTO);
//                })
//        );
//    }

    @Test
    void deleteLobby_invalidSession_sendsErrorMessage() {
        // Setup
        when(headerAccessor.getSessionId()).thenReturn(null);

        // Test
        lobbyWebSocketController.deleteLobby(1L, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby-manager/delete/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "LOBBY_DELETE_ERROR".equals(wsMsg.getType()) &&
                            "Invalid session".equals(wsMsg.getPayload());
                })
        );
    }

//    @Test
//    void joinLobbyByCode_success_sendsJoinSuccessMessage() {
//        // Setup
//        WebSocketMessage<Void> message = new WebSocketMessage<>();
//
//        LobbyJoinResponseDTO joinResponseDTO = new LobbyJoinResponseDTO();
//        joinResponseDTO.setMessage("Joined successfully");
//        joinResponseDTO.setLobby(lobbyResponseDTO);
//
//        when(lobbyService.joinLobby(1L, 1L, null, "TEST123", false)).thenReturn(joinResponseDTO);
//
//        // Test
//        lobbyWebSocketController.joinLobbyByCode("TEST123", message, principal, headerAccessor);
//
//        // Verify
//        verify(lobbyService).joinLobby(1L, 1L, null, "TEST123", false);
//        verify(webSocketEventListener).registerSessionWithLobby("test-session-id", 1L);
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(testToken),
//                eq("/topic/lobby/join/result"),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "JOIN_SUCCESS".equals(wsMsg.getType()) &&
//                            wsMsg.getPayload() instanceof LobbyDTO;
//                })
//        );
//        verify(messagingTemplate).convertAndSend(
//                Optional.ofNullable(eq("/topic/lobby/1/users")),
//                argThat(msg -> {
//                    if (!(msg instanceof WebSocketMessage)) return false;
//                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
//                    return "USER_JOINED".equals(wsMsg.getType()) &&
//                            userPublicDTO.equals(wsMsg.getPayload());
//                })
//        );
//    }

    @Test
    void joinLobbyByCode_lobbyNotFound_sendsErrorMessage() {
        // Setup
        WebSocketMessage<Void> message = new WebSocketMessage<>();

        when(lobbyService.getLobbyByCode("INVALID")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        // Test
        lobbyWebSocketController.joinLobbyByCode("INVALID", message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/join/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "JOIN_ERROR".equals(wsMsg.getType()) &&
                            "Lobby not found".equals(wsMsg.getPayload());
                })
        );
    }

    @Test
    void joinLobbyByCode_lobbyFull_sendsErrorMessage() {
        // Setup
        WebSocketMessage<Void> message = new WebSocketMessage<>();

        when(lobbyService.joinLobby(1L, 1L, null, "TEST123", false))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby is full"));

        // Test
        lobbyWebSocketController.joinLobbyByCode("TEST123", message, principal, headerAccessor);

        // Verify
        verify(messagingTemplate).convertAndSendToUser(
                eq(testToken),
                eq("/topic/lobby/join/result"),
                argThat(msg -> {
                    if (!(msg instanceof WebSocketMessage)) return false;
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "JOIN_ERROR".equals(wsMsg.getType()) &&
                            "Lobby is full".equals(wsMsg.getPayload());
                })
        );
    }
}