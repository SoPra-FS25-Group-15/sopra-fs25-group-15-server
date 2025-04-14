package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyManagementDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;

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
    
    /**
     * Helper method to validate that the user is authenticated
     */
    private Long validateAuthentication(Principal principal) {
        if (principal == null) {
            log.error("Unauthorized WebSocket request - missing principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("Invalid principal format: {}", principal.getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
        }
    }

    /**
     * Get all available lobbies when a client subscribes
     */
    @SubscribeMapping("/lobby-manager/lobbies")
    public WebSocketMessage<List<LobbyResponseDTO>> getAllLobbies(Principal principal) {
        validateAuthentication(principal);
        try {
            List<Lobby> lobbies = lobbyService.listLobbies();
            List<LobbyResponseDTO> lobbyDTOs = lobbies.stream()
                .map(mapper::lobbyEntityToResponseDTO)
                .collect(Collectors.toList());
            return new WebSocketMessage<>("LOBBIES_LIST", lobbyDTOs);
        } catch (Exception e) {
            log.error("Error retrieving lobbies: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBIES_ERROR", List.of());
        }
    }

    /**
     * Handle join lobby requests via WebSocket
     */
    @MessageMapping("/lobby-manager/join/{code}")
    public void joinLobbyByCode(@DestinationVariable String code,
                              @Payload WebSocketMessage<Void> message,
                              Principal principal) {
        
        Long userId = validateAuthentication(principal);
        
        try {
            log.info("User {} attempting to join lobby with code {}", userId, code);
            
            // Find the lobby by code
            Lobby lobby = lobbyService.getLobbyByCode(code);
            if (lobby == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
            }
            
            // Join the lobby
            LobbyJoinResponseDTO response = lobbyService.joinLobby(
                lobby.getId(), userId, null, code, false);
            
            // Send confirmation to the user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/join/result",
                new WebSocketMessage<>("JOIN_SUCCESS", response)
            );
            log.info("User {} successfully joined lobby {}", principal.getName(), code);
            
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
                new WebSocketMessage<>("JOIN_ERROR", 
                    Map.of("code", 500, "message", "An unexpected error occurred"))
            );
        }
    }

    /**
     * Get user's current lobby management state
     */
    @SubscribeMapping("/lobby-manager/state")
    public WebSocketMessage<LobbyManagementDTO> getLobbyManagementState(Principal principal) {
        Long userId = validateAuthentication(principal);

        try {
            LobbyManagementDTO state = new LobbyManagementDTO();
            Lobby currentLobby = lobbyService.getCurrentLobbyForUser(userId);

            if (currentLobby != null) {
                state.setCurrentLobbyCode(currentLobby.getLobbyCode());
            }

            List<LobbyManagementDTO.PendingInvite> pendingInvites =
                lobbyService.getPendingInvitesForUser(userId).stream()
                .map(invite -> {
                    LobbyManagementDTO.PendingInvite pendingInvite = new LobbyManagementDTO.PendingInvite();
                    pendingInvite.setUsername(invite.getSender().getProfile().getUsername());
                    pendingInvite.setLobbyCode(invite.getLobbyCode());
                    return pendingInvite;
                })
                .collect(Collectors.toList());

            state.setPendingInvites(pendingInvites);

            return new WebSocketMessage<>("LOBBY_MANAGEMENT_STATE", state);
        }
        catch (Exception e) {
            log.error("Error getting lobby management state: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBY_MANAGEMENT_ERROR", null);
        }
    }

    /**
     * Get detailed status of a specific lobby
     */
    @SubscribeMapping("/lobby-manager/lobby/{lobbyId}")
    public WebSocketMessage<?> getLobbyStatus(@DestinationVariable Long lobbyId,
                                              Principal principal) {
        try {
            Long userId = validateAuthentication(principal);
            log.info("User {} requesting status for lobby {}", userId, lobbyId);

            Lobby lobby = lobbyService.getLobbyById(lobbyId);

            if (!lobbyService.isUserInLobby(userId, lobbyId) && lobby.isPrivate()) {
                log.warn("User {} attempted to access private lobby {} they're not a member of", userId, lobbyId);
                return new WebSocketMessage<>("LOBBY_STATUS_ERROR", Map.of(
                    "code", HttpStatus.FORBIDDEN.value(),
                    "message", "You don't have permission to view this lobby"
                ));
            }

            LobbyResponseDTO lobbyDTO = mapper.lobbyEntityToResponseDTO(lobby);
            return new WebSocketMessage<>("LOBBY_STATUS", lobbyDTO);
        }
        catch (ResponseStatusException e) {
            log.error("Error retrieving lobby status: {} - {}", e.getStatus(), e.getMessage());
            return new WebSocketMessage<>("LOBBY_STATUS_ERROR", Map.of(
                "code", e.getStatus().value(),
                "message", e.getReason()
            ));
        }
        catch (Exception e) {
            log.error("Unexpected error retrieving lobby status: {}", e.getMessage());
            return new WebSocketMessage<>("LOBBY_STATUS_ERROR", Map.of(
                "code", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "message", "An unexpected error occurred"
            ));
        }
    }


    /**
     * Send a lobby invite
     */
    @MessageMapping("/lobby-manager/invite")
    public void sendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                Principal principal) {
        Long senderId = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        
        try {
            log.info("User {} sending lobby invite to {}", senderId, toUsername);
            
            // Get the sender user
            User sender = userService.getPublicProfile(senderId);
            
            // Find recipient by username
            User recipient = userService.findByUsername(toUsername);
            if (recipient == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            
            // Get current lobby code for the sender
            Lobby currentLobby = lobbyService.getCurrentLobbyForUser(senderId);
            if (currentLobby == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must be in a lobby to invite others");
            }
            
            String lobbyCode = currentLobby.getLobbyCode();
            
            // Create and store the invite in the service
            lobbyService.createLobbyInvite(sender, recipient, lobbyCode);
            
            // Send confirmation to the sender
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/result",
                new WebSocketMessage<>("INVITE_SENT", Map.of(
                    "recipient", toUsername,
                    "lobbyCode", lobbyCode
                ))
            );
            
            // Send notification to the recipient
            messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/topic/lobby-manager/invites",
                new WebSocketMessage<>("INVITE_IN", Map.of(
                    "fromUsername", sender.getProfile().getUsername(),
                    "lobbyCode", lobbyCode
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
     * Cancel a pending lobby invite
     */
    @MessageMapping("/lobby-manager/invite/cancel")
    public void cancelLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                  Principal principal) {
        Long senderId = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String toUsername = payload.get("toUsername");
        
        try {
            log.info("User {} canceling lobby invite to {}", senderId, toUsername);
            
            // Get the sender and recipient
            User sender = userService.getPublicProfile(senderId);
            User recipient = userService.findByUsername(toUsername);
            
            if (recipient == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            
            // Cancel the invite
            boolean canceled = lobbyService.cancelLobbyInvite(sender, recipient);
            
            if (!canceled) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending invite found");
            }
            
            // Send confirmation to the sender
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/cancel/result",
                new WebSocketMessage<>("INVITE_CANCELED", Map.of("recipient", toUsername))
            );
            
            // Notify the recipient that the invite was canceled
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
     * Accept a friend invite to join their lobby
     * This endpoint specifically handles friend invites and doesn't expect a lobby code from clients
     */
    @MessageMapping("/lobby-manager/invite/accept")
    public void acceptFriendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                             Principal principal,
                             StompHeaderAccessor headerAccessor) {
        Long recipientId = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String fromUsername = payload.get("fromUsername");
        
        try {
            log.info("User {} accepting friend invite from {}", recipientId, fromUsername);
            
            // Additional check for session information
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.error("Invalid session for friend invite accept request");
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/lobby-manager/invite/accept/result",
                    new WebSocketMessage<>("INVITE_ACCEPT_ERROR", Map.of(
                        "code", HttpStatus.UNAUTHORIZED.value(),
                        "message", "Invalid session"
                    ))
                );
                return;
            }
            
            // Find the sender by username
            User sender = userService.findByUsername(fromUsername);
            if (sender == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviting user not found");
            }
            
            // Get the recipient
            User recipient = userService.getPublicProfile(recipientId);
            
            // For friend invites, we always look up the sender's current lobby
            log.info("Looking up current lobby of friend: {}", fromUsername);
            Lobby senderLobby = lobbyService.getCurrentLobbyForUser(sender.getId());
            if (senderLobby == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Your friend is not currently in any lobby");
            }
            String lobbyCode = senderLobby.getLobbyCode();
            log.info("Found friend's lobby with code: {}", lobbyCode);
            
            // Verify that an invite exists from this sender
            if (!lobbyService.hasAnyPendingInviteFrom(sender, recipient)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No invite found from this friend");
            }
            
            // Join the friend's lobby
            LobbyJoinResponseDTO response = lobbyService.joinLobby(
                senderLobby.getId(), recipientId, null, lobbyCode, true);
            
            // Register this user's session with the lobby
            webSocketEventListener.registerSessionWithLobby(sessionId, senderLobby.getId());
            
            // Remove the invite as it's been accepted
            lobbyService.cancelLobbyInvite(sender, recipient);
            
            // Send confirmation to the recipient
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/accept/result",
                new WebSocketMessage<>("INVITE_ACCEPTED", response)
            );
            
            // Notify the original sender that their invite was accepted
            messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/topic/lobby-manager/invites/status",
                new WebSocketMessage<>("INVITE_ACCEPTED", Map.of(
                    "username", recipient.getProfile().getUsername(),
                    "lobbyCode", lobbyCode
                ))
            );
            
            // Broadcast to the lobby that a new user has joined
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + senderLobby.getId() + "/users",
                new WebSocketMessage<>("USER_JOINED", mapper.toUserPublicDTO(recipient))
            );
            
        } catch (ResponseStatusException e) {
            log.error("Error accepting friend invite: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/accept/result",
                new WebSocketMessage<>("INVITE_ACCEPT_ERROR", Map.of(
                    "code", e.getStatus().value(),
                    "message", e.getReason()
                ))
            );
        } catch (Exception e) {
            log.error("Unexpected error accepting friend invite: {}", e.getMessage());
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
     * Decline a friend's lobby invite
     */
    @MessageMapping("/lobby-manager/invite/decline")
    public void declineFriendLobbyInvite(@Payload WebSocketMessage<Map<String, String>> message,
                                    Principal principal,
                                    StompHeaderAccessor headerAccessor) {
        Long recipientId = validateAuthentication(principal);
        Map<String, String> payload = message.getPayload();
        String fromUsername = payload.get("fromUsername");
        
        try {
            log.info("User {} declining invite from {}", recipientId, fromUsername);
            
            // Additional check for session information
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.error("Invalid session for invite decline request");
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/lobby-manager/invite/decline/result",
                    new WebSocketMessage<>("INVITE_DECLINE_ERROR", Map.of(
                        "code", HttpStatus.UNAUTHORIZED.value(),
                        "message", "Invalid session"
                    ))
                );
                return;
            }
            
            // Find the sender by username
            User sender = userService.findByUsername(fromUsername);
            if (sender == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inviting user not found");
            }
            
            // Get the recipient (current user)
            User recipient = userService.getPublicProfile(recipientId);
            
            // Verify that an invite exists from this sender
            if (!lobbyService.hasAnyPendingInviteFrom(sender, recipient)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No invite found from this friend");
            }
            
            // Remove the invite as it's been declined
            lobbyService.cancelLobbyInvite(sender, recipient);
            
            // Send confirmation to the recipient that the decline was processed
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/invite/decline/result",
                new WebSocketMessage<>("INVITE_DECLINED", Map.of(
                    "fromUsername", fromUsername,
                    "message", "Invite declined successfully"
                ))
            );
            
            // Notify the original sender that their invite was declined
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
