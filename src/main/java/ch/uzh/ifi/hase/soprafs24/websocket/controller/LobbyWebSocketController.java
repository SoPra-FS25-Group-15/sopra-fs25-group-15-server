package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GenericMessageResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketEventListener;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;

@Controller
public class LobbyWebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketController.class);
    
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
    private AuthService authService;

    /**
     * Validate user authentication from WebSocket principal
     * @param principal The Principal from the WebSocket session
     * @return The validated user token
     * @throws ResponseStatusException if authentication fails
     */
    private String validateAuthentication(Principal principal) {
        if (principal == null) {
            log.error("Unauthorized WebSocket request - missing principal");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        String principalName = principal.getName();
        log.debug("Validating authentication for principal: {}", principalName);
        
        // If the principal is a numeric user ID, try to find the token
        if (principalName.matches("\\d+")) {
            try {
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
        String token = TokenUtils.extractToken(principalName);
        User user = authService.getUserByToken(token);
        if (user == null) {
            log.error("Invalid token: {}", TokenUtils.maskToken(token));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication");
        }
        log.debug("Authentication succeeded for user ID: {}", user.getId());
        return token;
    }

    /**
     * Extract token from StompHeaderAccessor as a fallback
     */
    private String getUserTokenFromHeaders(StompHeaderAccessor headerAccessor) {
        if (headerAccessor == null) {
            return null;
        }
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
            String token = (String) sessionAttributes.get("token");
            log.debug("Found token in session attributes: {}", TokenUtils.maskToken(token));
            return token;
        }
        
        List<String> authHeaders = headerAccessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String token = TokenUtils.extractToken(authHeaders.get(0));
            log.debug("Found token in Authorization header: {}", TokenUtils.maskToken(token));
            return token;
        }
        
        return null;
    }

    /**
     * Create a new lobby
     */
    @MessageMapping("/lobby/create")
    public void createLobby(@Payload WebSocketMessage<LobbyRequestDTO> message, 
                            Principal principal,
                            StompHeaderAccessor headerAccessor) {
        String token = validateAuthentication(principal);
        User me = authService.getUserByToken(token);
        Long userId = me.getId();
        
        try {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.error("Invalid session for lobby creation request");
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/lobby/create/result",
                    new WebSocketMessage<>("LOBBY_CREATE_ERROR", "Invalid session")
                );
                return;
            }
            
            log.info("User {} creating new lobby", userId);
            
            Lobby lobbyEntity = mapper.lobbyRequestDTOToEntity(message.getPayload());
            lobbyEntity.setHost(userService.getPublicProfile(userId));
            Lobby createdLobby = lobbyService.createLobby(lobbyEntity);
            
            LobbyDTO lobbyDTO = new LobbyDTO();
            lobbyDTO.setCode(createdLobby.getLobbyCode());
            lobbyDTO.setMaxPlayers(String.valueOf(createdLobby.getMaxPlayers()));
            lobbyDTO.setPlayersPerTeam(createdLobby.getMaxPlayersPerTeam());
            if (createdLobby.getHintsEnabled() != null) {
                lobbyDTO.setRoundCardsStartAmount(createdLobby.getHintsEnabled().size());
            }
            
            webSocketEventListener.registerSessionWithLobby(sessionId, createdLobby.getId());
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/create/result",
                new WebSocketMessage<>("LOBBY_CREATED", lobbyDTO)
            );
            messagingTemplate.convertAndSend(
                "/topic/lobbies",
                new WebSocketMessage<>("LOBBY_ADDED", lobbyDTO)
            );
            
        } catch (Exception e) {
            log.error("Error creating lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/create/result",
                new WebSocketMessage<>("LOBBY_CREATE_ERROR", e.getMessage())
            );
        }
    }
    
    /**
     * Handle lobby update settings
     * Type "UPDATE"
     */
    @Transactional
    @MessageMapping("/lobby/{lobbyId}/update")
    public void updateLobbySettings(@DestinationVariable Long lobbyId, 
                                    @Payload WebSocketMessage<LobbyDTO> message,
                                    Principal principal,
                                    StompHeaderAccessor headerAccessor) {
        String token = validateAuthentication(principal);
        User me = authService.getUserByToken(token);
        Long userId = me.getId();
        log.info("User {} attempting to update lobby {}", userId, lobbyId);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.error("Invalid session for lobby update request");
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/lobby/update/error",
                    new WebSocketMessage<>("UPDATE_ERROR", "Invalid session")
                );
                return;
            }
            
            LobbyConfigUpdateRequestDTO updateRequest = new LobbyConfigUpdateRequestDTO();
            LobbyDTO lobbyData = message.getPayload();
            
            if (lobbyData.getMaxPlayers() != null) {
                try {
                    updateRequest.setMaxPlayers(Integer.parseInt(lobbyData.getMaxPlayers()));
                } catch (NumberFormatException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maxPlayers format");
                }
            }
            if (lobbyData.getPlayersPerTeam() != null) {
                updateRequest.setMaxPlayersPerTeam(lobbyData.getPlayersPerTeam());
            }
            
            Lobby updatedLobby = lobbyService.updateLobbyConfig(lobbyId, updateRequest, userId);
            if (updatedLobby.getHintsEnabled() != null) {
                Hibernate.initialize(updatedLobby.getHintsEnabled());
            }
            
            LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(updatedLobby);
            LobbyDTO responseLobbyDTO = convertToLobbyDTO(responseDTO);
            
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId, 
                new WebSocketMessage<>("UPDATE_SUCCESS", responseLobbyDTO)
            );
            
        } catch (ResponseStatusException e) {
            String err = switch (e.getStatus()) {
                case FORBIDDEN -> "Only the host can update lobby configuration";
                case BAD_REQUEST -> "Invalid parameters submitted.";
                default -> e.getMessage();
            };
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/update/error",
                new WebSocketMessage<>("UPDATE_ERROR", err)
            );
        } catch (Exception e) {
            log.error("Error updating lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/update/error",
                new WebSocketMessage<>("UPDATE_ERROR", "An unexpected error occurred")
            );
        }
    }
    
    /**
     * Handle leave lobby
     * Type "LEAVE"
     */
    @MessageMapping("/lobby/{lobbyId}/leave")
    public void leaveLobby(@DestinationVariable Long lobbyId,
                           @Payload WebSocketMessage<Map<String, String>> message,
                           Principal principal,
                           StompHeaderAccessor headerAccessor) {
        String token = validateAuthentication(principal);
        User me = authService.getUserByToken(token);
        Long userId = me.getId();
        log.info("User {} leaving lobby {}", userId, lobbyId);
        
        try {
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                webSocketEventListener.unregisterSessionFromLobby(sessionId);
            }
            
            boolean isHost = lobbyService.isUserHost(lobbyId, userId);
            if (isHost) {
                GenericMessageResponseDTO response = lobbyService.deleteLobby(lobbyId, userId);
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), 
                    "/topic/lobby/leave/result", 
                    new WebSocketMessage<>("LEAVE_SUCCESS", null)
                );
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId,
                    new WebSocketMessage<>("LOBBY_DISBANDED", Map.of("message", "Lobby disbanded because host left"))
                );
                messagingTemplate.convertAndSend(
                    "/topic/lobby-manager/lobbies",
                    new WebSocketMessage<>("LOBBY_REMOVED", lobbyId)
                );
            } else {
                LobbyLeaveResponseDTO response = lobbyService.leaveLobby(lobbyId, userId, userId);
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), 
                    "/topic/lobby/leave/result", 
                    new WebSocketMessage<>("LEAVE_SUCCESS", convertToLobbyDTO(response.getLobby()))
                );
                messagingTemplate.convertAndSend(
                    "/topic/lobby/" + lobbyId + "/users", 
                    new WebSocketMessage<>("USER_LEFT", Map.of("userId", userId))
                );
            }
            
        } catch (ResponseStatusException e) {
            String err = switch (e.getStatus()) {
                case FORBIDDEN -> "Only the host can kick players";
                case BAD_REQUEST -> "User not in lobby";
                default -> e.getMessage();
            };
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/topic/lobby/leave/result", 
                new WebSocketMessage<>("LEAVE_ERROR", err)
            );
        } catch (Exception e) {
            log.error("Error leaving lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/topic/lobby/leave/result", 
                new WebSocketMessage<>("LEAVE_ERROR", e.getMessage())
            );
        }
    }
    
    /**
     * Delete a lobby
     */
    @MessageMapping("/lobby-manager/delete/{lobbyId}")
    public void deleteLobby(@DestinationVariable Long lobbyId, 
                            Principal principal, 
                            StompHeaderAccessor headerAccessor) {
        String token = validateAuthentication(principal);
        User me = authService.getUserByToken(token);
        Long userId = me.getId();
        
        try {
            log.info("User {} attempting to delete lobby {}", userId, lobbyId);
            
            String sessionId = headerAccessor.getSessionId();
            if (sessionId == null) {
                log.error("Invalid session for lobby deletion request");
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/topic/lobby-manager/delete/result",
                    new WebSocketMessage<>("LOBBY_DELETE_ERROR", "Invalid session")
                );
                return;
            }
            
            GenericMessageResponseDTO response = lobbyService.deleteLobby(lobbyId, userId);
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/delete/result",
                new WebSocketMessage<>("LOBBY_DELETED", response)
            );
            messagingTemplate.convertAndSend(
                "/topic/lobby-manager/lobbies",
                new WebSocketMessage<>("LOBBY_REMOVED", lobbyId)
            );
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobbyId,
                new WebSocketMessage<>("LOBBY_DISBANDED", response)
            );
            
        } catch (Exception e) {
            log.error("Error deleting lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby-manager/delete/result",
                new WebSocketMessage<>("LOBBY_DELETE_ERROR", e.getMessage())
            );
        }
    }
    
    /**
     * Join lobby by code
     */
    @MessageMapping("/lobby/join/{code}")
    public void joinLobbyByCode(@DestinationVariable String code,
                                @Payload(required = false) WebSocketMessage<Void> message,
                                Principal principal,
                                StompHeaderAccessor headerAccessor) {
        String userToken;
        try {
            userToken = validateAuthentication(principal);
        } catch (Exception e) {
            log.warn("Principal auth failed, trying header accessor: {}", e.getMessage());
            userToken = getUserTokenFromHeaders(headerAccessor);
            if (userToken == null) {
                log.error("Authentication failed - no valid token found");
                messagingTemplate.convertAndSend(
                    "/user/topic/lobby/join/result",
                    new WebSocketMessage<>("JOIN_ERROR", "Authentication required")
                );
                return;
            }
            authService.getUserByToken(userToken); // will throw if invalid
        }
        
        try {
            log.info("Attempting to join lobby with code {}", code);
            
            User me = authService.getUserByToken(userToken);
            Long userId = me.getId();
            Lobby lobby = lobbyService.getLobbyByCode(code);
            LobbyJoinResponseDTO response = lobbyService.joinLobby(
                lobby.getId(), userId, null, code, false);
                
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/join/result",
                new WebSocketMessage<>("JOIN_SUCCESS", convertToLobbyDTO(response.getLobby()))
            );
            
            String sessionId = headerAccessor.getSessionId();
            if (sessionId != null) {
                webSocketEventListener.registerSessionWithLobby(sessionId, lobby.getId());
            }
            
            messagingTemplate.convertAndSend(
                "/topic/lobby/" + lobby.getId() + "/users",
                new WebSocketMessage<>("USER_JOINED", mapper.toUserPublicDTO(me))
            );
            
        } catch (ResponseStatusException e) {
            String errorMessage;
            switch (e.getStatus()) {
                case NOT_FOUND -> errorMessage = "Lobby not found";
                case BAD_REQUEST -> errorMessage = "Lobby is full";
                case FORBIDDEN -> errorMessage = "You need to be logged in to join a lobby.";
                default -> errorMessage = e.getMessage();
            }
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/join/result",
                new WebSocketMessage<>("JOIN_ERROR", errorMessage)
            );
        } catch (Exception e) {
            log.error("Unexpected error joining lobby: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/topic/lobby/join/result",
                new WebSocketMessage<>("JOIN_ERROR", "An unexpected error occurred")
            );
        }
    }
    
    /**
     * Convert a REST LobbyResponseDTO into a WebSocket LobbyDTO
     */
    private LobbyDTO convertToLobbyDTO(LobbyResponseDTO responseDTO) {
        if (responseDTO == null) {
            return null;
        }
        LobbyDTO dto = new LobbyDTO();
        dto.setCode(responseDTO.getCode());
        dto.setMaxPlayers(responseDTO.getMaxPlayers());
        dto.setPlayersPerTeam(responseDTO.getPlayersPerTeam());
        dto.setRoundCardsStartAmount(responseDTO.getRoundCardsStartAmount());
        return dto;
    }
}
