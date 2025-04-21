package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyManagementDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketLobbyStatusDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;

@Controller
public class LobbyManagerWebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(LobbyManagerWebSocketController.class);
    
    @Autowired
    private LobbyService lobbyService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DTOMapper mapper;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private WebSocketEventListener webSocketEventListener;
    
    @Autowired
    private AuthService authService; // Add missing AuthService autowired field
    
    /**
     * Helper to validate that the user is authenticated.
     */
    private String validateAuthentication(Principal principal) {
        if (principal == null) {
            log.error("Unauthorized WebSocket request - missing principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        String principalName = principal.getName();
        log.debug("Validating authentication for principal: {}", principalName);
        
        // If the principal is a numeric user ID, try to find the token
        if (principalName != null && principalName.matches("\\d+")) {
            log.debug("Principal appears to be a user ID: {}. Finding token.", principalName);
            
            try {
                // Find the user
                User user = userService.getPublicProfile(Long.parseLong(principalName));
                if (user != null && user.getToken() != null) {
                    log.debug("Found token for user ID {}", principalName);
                    return user.getToken();
                }
            } catch (Exception e) {
                log.warn("Error retrieving token for user ID {}: {}", principalName, e.getMessage());
            }
        }
        
        // Otherwise the principal name should be the token itself
        try {
            // Extract token if it starts with Bearer
            String token = TokenUtils.extractToken(principalName);
            User user = authService.getUserByToken(token);
            
            if (user == null) {
                log.error("Invalid token: {}", TokenUtils.maskToken(token));
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
            }
            
            log.debug("Authentication succeeded for user ID: {}", user.getId());
            return token;
        } catch (ResponseStatusException e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Send all lobbies on initial subscribe.
     */
    @Transactional(readOnly = true)
    @SubscribeMapping("/lobby-manager/lobbies")
    public WebSocketMessage<List<LobbyResponseDTO>> getAllLobbies(Principal principal) {
        String userToken = validateAuthentication(principal);
        try {
            // Validate the token
            authService.getUserByToken(userToken);
            
            List<Lobby> lobbies = lobbyService.listLobbies();
            lobbies.forEach(lobby -> Hibernate.initialize(lobby.getHintsEnabled()));
            List<LobbyResponseDTO> dtos = lobbies.stream()
                .map(mapper::lobbyEntityToResponseDTO)
                .collect(Collectors.toList());
            return new WebSocketMessage<>("LOBBIES_LIST", dtos);
        } catch (Exception e) {
            log.error("Error retrieving lobbies: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBIES_ERROR", List.of());
        }
    }
    
    /**
     * Handle join lobby by code requests.
     */
    @MessageMapping("/lobby-manager/join/{code}")
    public void joinLobbyByCode(@DestinationVariable String code,
                                @Payload WebSocketMessage<Void> message,
                                Principal principal) {
        String userToken = validateAuthentication(principal);
        try {
            // Convert token to userId for methods that expect a Long
            User user = authService.getUserByToken(userToken);
            Long userId = user.getId();
            
            log.info("User {} attempting to join lobby with code {}", userId, code);
            Lobby lobby = lobbyService.getLobbyByCode(code);
            LobbyJoinResponseDTO response = lobbyService.joinLobby(
                lobby.getId(), userId, null, code, false);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/join/result",
                new WebSocketMessage<>("JOIN_SUCCESS", response)
            );
        } catch (ResponseStatusException e) {
            log.error("Error joining lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/join/result",
                new WebSocketMessage<>(
                    "JOIN_ERROR",
                    Map.of("code", e.getStatus().value(), "message", e.getReason())
                )
            );
        } catch (Exception e) {
            log.error("Unexpected error joining lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/join/result",
                new WebSocketMessage<>(
                    "JOIN_ERROR",
                    Map.of("code", 500, "message", "An unexpected error occurred")
                )
            );
        }
    }
    
    /**
     * Send the user's current lobby code and pending invites on subscribe.
     */
    @SubscribeMapping("/lobby-manager/state")
    public WebSocketMessage<LobbyManagementDTO> getLobbyManagementState(Principal principal) {
        String userToken = validateAuthentication(principal);
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long userId = user.getId();
            
            LobbyManagementDTO state = new LobbyManagementDTO();
            Lobby current = lobbyService.getCurrentLobbyForUser(userId);
            if (current != null) {
                state.setCurrentLobbyCode(current.getLobbyCode());
            }
            var pending = lobbyService.getPendingInvitesForUser(userId).stream()
                .map(inv -> {
                    var p = new LobbyManagementDTO.PendingInvite();
                    p.setUsername(inv.getSender().getProfile().getUsername());
                    p.setLobbyCode(inv.getLobbyCode());
                    return p;
                })
                .collect(Collectors.toList());
            state.setPendingInvites(pending);
            return new WebSocketMessage<>("LOBBY_MANAGEMENT_STATE", state);
        } catch (Exception e) {
            log.error("Error getting lobby management state: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBY_MANAGEMENT_ERROR", null);
        }
    }

    /**
     * Retrieve detailed status of a lobby by its code.
     * Now returns a full WebSocketLobbyStatusDTO (with host and players).
     */
    @Transactional(readOnly = true)
    @SubscribeMapping("/lobby-manager/lobby/{lobbyCode}")
    public WebSocketMessage<WebSocketLobbyStatusDTO> getLobbyStatusByCode(
            @DestinationVariable String lobbyCode,
            Principal principal) {

        String userToken = validateAuthentication(principal);
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long userId = user.getId();
            
            log.info("User {} requesting status for lobby '{}'", userId, lobbyCode);
            Lobby lobby = lobbyService.getLobbyByCode(lobbyCode);

            if (lobby.isPrivate() && !lobbyService.isUserInLobby(userId, lobby.getId())) {
                log.warn("Unauthorized access to private lobby '{}'", lobbyCode);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
            }

            // Initialize lazy collections
            if (lobby.getPlayers() != null) {
                Hibernate.initialize(lobby.getPlayers());
            }
            if (lobby.getTeams() != null) {
                Hibernate.initialize(lobby.getTeams());
                lobby.getTeams().values().forEach(Hibernate::initialize);
            }

            // Map host and players
            UserPublicDTO hostDto = mapper.toUserPublicDTO(lobby.getHost());
            List<UserPublicDTO> playerDtos = (lobby.getPlayers() != null)
                ? lobby.getPlayers().stream()
                        .map(mapper::toUserPublicDTO)
                        .collect(Collectors.toList())
                : Collections.emptyList();

            WebSocketLobbyStatusDTO payload = new WebSocketLobbyStatusDTO(
                lobby.getId(),
                lobby.getLobbyCode(),
                hostDto,
                lobby.getMode(),
                String.valueOf(lobby.getMaxPlayers()),
                lobby.getMaxPlayersPerTeam(),
                (lobby.getHintsEnabled() != null ? lobby.getHintsEnabled().size() : null),
                lobby.isPrivate(),
                lobby.getStatus(),
                playerDtos
            );

            return new WebSocketMessage<>("LOBBY_STATUS", payload);

        } catch (ResponseStatusException e) {
            log.error("Error retrieving lobby status: {} – {}", e.getStatus(), e.getReason());
            return new WebSocketMessage<>("LOBBY_STATUS_ERROR", null);
        } catch (Exception e) {
            log.error("Unexpected error retrieving lobby status: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBY_STATUS_ERROR", null);
        }
    }
    
    /**
     * Send a lobby invite.
     */
    @MessageMapping("/lobby-manager/invite")
    public void sendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                Principal principal) {
        String userToken = validateAuthentication(principal);
        String toUsername = message.getPayload().get("toUsername");
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long senderId = user.getId();
            
            log.info("User {} sending lobby invite to {}", senderId, toUsername);
            User sender = userService.getPublicProfile(senderId);
            User tmp = userService.findByUsername(toUsername);
            if (tmp == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            User recipient = userService.getPublicProfile(tmp.getId());
            Lobby current = lobbyService.getCurrentLobbyForUser(senderId);
            if (current == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must be in lobby to invite");
            }
            String code = current.getLobbyCode();
            lobbyService.createLobbyInvite(sender, recipient, code);

            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/result",
                new WebSocketMessage<>("INVITE_SENT", Map.of(
                    "recipient", toUsername,
                    "lobbyCode", code
                ))
            );
            messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/topic/lobby-manager/invites",
                new WebSocketMessage<>("INVITE_IN", Map.of(
                    "fromUsername", sender.getProfile().getUsername(),
                    "lobbyCode", code
                ))
            );
        } catch (ResponseStatusException e) {
            log.error("Error sending invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/result",
                new WebSocketMessage<>("INVITE_ERROR", Map.of(
                    "code", e.getStatus().value(),
                    "message", e.getReason()
                ))
            );
        } catch (Exception e) {
            log.error("Unexpected error sending invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/result",
                new WebSocketMessage<>("INVITE_ERROR", Map.of(
                    "code", 500,
                    "message", "An unexpected error occurred"
                ))
            );
        }
    }

    /**
     * Cancel a pending lobby invite.
     */
    @MessageMapping("/lobby-manager/invite/cancel")
    public void cancelLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                  Principal principal) {
        String userToken = validateAuthentication(principal);
        String toUsername = message.getPayload().get("toUsername");
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long senderId = user.getId();
            
            log.info("User {} canceling invite to {}", senderId, toUsername);
            User sender = userService.getPublicProfile(senderId);
            User tmp = userService.findByUsername(toUsername);
            if (tmp == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            User recipient = userService.getPublicProfile(tmp.getId());
            boolean canceled = lobbyService.cancelLobbyInvite(sender, recipient);
            if (!canceled) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending invite");
            }
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/cancel/result",
                new WebSocketMessage<>("INVITE_CANCELED", Map.of("recipient", toUsername))
            );
            messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/topic/lobby-manager/invites",
                new WebSocketMessage<>("INVITE_CANCELED", Map.of(
                    "fromUsername", sender.getProfile().getUsername()
                ))
            );
        } catch (ResponseStatusException e) {
            log.error("Error canceling invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/cancel/result",
                new WebSocketMessage<>("INVITE_CANCEL_ERROR", Map.of(
                    "code", e.getStatus().value(),
                    "message", e.getReason()
                ))
            );
        } catch (Exception e) {
            log.error("Unexpected error canceling invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/cancel/result",
                new WebSocketMessage<>("INVITE_CANCEL_ERROR", Map.of(
                    "code", 500,
                    "message", "An unexpected error occurred"
                ))
            );
        }
    }

    /**
     * Accept a friend's lobby invite.
     */
    @MessageMapping("/lobby-manager/invite/accept")
    public void acceptFriendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                        Principal principal,
                                        org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor) {
        String userToken = validateAuthentication(principal);
        String fromUsername = message.getPayload().get("fromUsername");
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long recipientId = user.getId();
            
            log.info("User {} accepting invite from {}", recipientId, fromUsername);
            User sender = userService.findByUsername(fromUsername);
            if (sender == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviting user not found");
            }
            User recipient = userService.getPublicProfile(recipientId);
            Lobby senderLobby = lobbyService.getCurrentLobbyForUser(sender.getId());
            if (senderLobby == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not in lobby");
            }
            if (!lobbyService.hasAnyPendingInviteFrom(sender, recipient)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No invite found");
            }
            LobbyJoinResponseDTO response = lobbyService.joinLobby(
                senderLobby.getId(), recipientId, null, senderLobby.getLobbyCode(), true);
            lobbyService.cancelLobbyInvite(sender, recipient);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/accept/result",
                new WebSocketMessage<>("INVITE_ACCEPTED", response)
            );
            messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/topic/lobby-manager/invites/status",
                new WebSocketMessage<>("INVITE_ACCEPTED", Map.of(
                    "username", recipient.getProfile().getUsername(),
                    "lobbyCode", senderLobby.getLobbyCode()
                ))
            );
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + senderLobby.getId() + "/users",
                new WebSocketMessage<>("USER_JOINED", mapper.toUserPublicDTO(recipient))
            );
        } catch (ResponseStatusException e) {
            log.error("Error accepting invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/accept/result",
                new WebSocketMessage<>("INVITE_ACCEPT_ERROR", Map.of(
                    "code", e.getStatus().value(),
                    "message", e.getReason()
                ))
            );
        } catch (Exception e) {
            log.error("Unexpected error accepting invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/accept/result",
                new WebSocketMessage<>("INVITE_ACCEPT_ERROR", Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "An unexpected error occurred"
                ))
            );
        }
    }

    /**
     * Decline a friend's lobby invite.
     */
    @MessageMapping("/lobby-manager/invite/decline")
    public void declineFriendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                         Principal principal,
                                         org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor) {
        String userToken = validateAuthentication(principal);
        String fromUsername = message.getPayload().get("fromUsername");
        try {
            // Convert token to userId
            User user = authService.getUserByToken(userToken);
            Long recipientId = user.getId();
            
            log.info("User {} declining invite from {}", recipientId, fromUsername);
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
            }
            User sender = userService.findByUsername(fromUsername);
            if (sender == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviting user not found");
            }
            User recipient = userService.getPublicProfile(recipientId);
            if (!lobbyService.hasAnyPendingInviteFrom(sender, recipient)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No invite found");
            }
            lobbyService.cancelLobbyInvite(sender, recipient);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/decline/result",
                new WebSocketMessage<>("INVITE_DECLINED", Map.of(
                    "fromUsername", fromUsername,
                    "message", "Invite declined successfully"
                ))
            );
            messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/topic/lobby-manager/invites/status",
                new WebSocketMessage<>("INVITE_DECLINED", Map.of(
                    "username", recipient.getProfile().getUsername(),
                    "message", "Your invite was declined"
                ))
            );
        } catch (ResponseStatusException e) {
            log.error("Error declining invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/decline/result",
                new WebSocketMessage<>("INVITE_DECLINE_ERROR", Map.of(
                    "code", e.getStatus().value(),
                    "message", e.getReason()
                ))
            );
        } catch (Exception e) {
            log.error("Unexpected error declining invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/decline/result",
                new WebSocketMessage<>("INVITE_DECLINE_ERROR", Map.of(
                    "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "message", "An unexpected error occurred"
                ))
            );
        }
    }
}
