package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GenericMessageResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.JoinLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyInviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;

@RestController
@RequestMapping("/lobbies")
public class LobbyController {

    private final LobbyService lobbyService;
    private final DTOMapper mapper;
    private final AuthService authService;

    @Autowired
    public LobbyController(LobbyService lobbyService, DTOMapper mapper, AuthService authService) {
        this.lobbyService = lobbyService;
        this.mapper = mapper;
        this.authService = authService;
    }

    // Create a new lobby.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyResponseDTO createLobby(@RequestHeader("Authorization") String token,
                                        @RequestBody LobbyRequestDTO lobbyRequestDTO) {
        User currentUser = authService.getUserByToken(token);
        Lobby lobby = mapper.lobbyRequestDTOToEntity(lobbyRequestDTO);
        lobby.setHost(currentUser);
        Lobby createdLobby = lobbyService.createLobby(lobby);
        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(createdLobby);
        // Ensure we have roundCardsStartAmount set
        if (responseDTO.getRoundCardsStartAmount() == null && createdLobby.getHintsEnabled() != null) {
            responseDTO.setRoundCardsStartAmount(createdLobby.getHintsEnabled().size());
        }
        return responseDTO;
    }

    // Retrieve lobby details.
    @GetMapping("/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    public LobbyResponseDTO getLobby(@PathVariable Long lobbyId) {
        Lobby lobby = lobbyService.getLobbyById(lobbyId);
        return mapper.lobbyEntityToResponseDTO(lobby);
    }

    // Update lobby configuration.
    @PutMapping("/{lobbyId}/config")
    @ResponseStatus(HttpStatus.OK)
    public LobbyResponseDTO updateLobbyConfig(@RequestHeader("Authorization") String token,
                                              @PathVariable Long lobbyId,
                                              @RequestBody LobbyConfigUpdateRequestDTO configUpdateDTO) {
        User currentUser = authService.getUserByToken(token);
        Lobby updatedLobby = lobbyService.updateLobbyConfig(lobbyId, configUpdateDTO, currentUser.getId());
        return mapper.lobbyEntityToResponseDTO(updatedLobby);
    }

    // Invite a player to the lobby.
    @PostMapping("/{lobbyId}/invite")
    @ResponseStatus(HttpStatus.OK)
    public LobbyInviteResponseDTO inviteToLobby(@RequestHeader("Authorization") String token,
                                                @PathVariable Long lobbyId,
                                                @RequestBody InviteLobbyRequestDTO inviteDTO) {
        User currentUser = authService.getUserByToken(token);
        return lobbyService.inviteToLobby(lobbyId, currentUser.getId(), inviteDTO);
    }

    // Delete a lobby.
    @DeleteMapping("/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    public GenericMessageResponseDTO deleteLobby(@RequestHeader("Authorization") String token,
                                                 @PathVariable Long lobbyId) {
        User currentUser = authService.getUserByToken(token);
        return lobbyService.deleteLobby(lobbyId, currentUser.getId());
    }

    // Join a lobby.
    @PostMapping("/{lobbyId}/join")
    public ResponseEntity<LobbyJoinResponseDTO> joinLobby(@PathVariable Long lobbyId,
                                                        @RequestParam Long userId,
                                                        @RequestBody JoinLobbyRequestDTO joinRequest) {
        // No longer extract team name - pass null for team parameter
        LobbyJoinResponseDTO response = lobbyService.joinLobby(
            lobbyId,
            userId,
            null, // No team name parameter
            joinRequest.getLobbyCode(),
            joinRequest.isFriendInvited()
        );
        return ResponseEntity.ok(response);
    }

    // Leave or kick from a lobby.
    @DeleteMapping("/{lobbyId}/leave")
    @ResponseStatus(HttpStatus.OK)
    public LobbyLeaveResponseDTO leaveLobby(@RequestHeader("Authorization") String token,
                                            @PathVariable Long lobbyId,
                                            @RequestParam(required = false) Long userId) {
        User currentUser = authService.getUserByToken(token);
        Long userToRemove = (userId != null) ? userId : currentUser.getId();
        return lobbyService.leaveLobby(lobbyId, currentUser.getId(), userToRemove);
    }

    // List all active lobbies
    @GetMapping("/all_lobbies")
    @ResponseStatus(HttpStatus.OK)
    public List<LobbyResponseDTO> getAllLobbies(@RequestHeader("Authorization") String token) {
        // Authenticate the user (token validation)
        authService.getUserByToken(token);
        
        // Get all lobbies
        List<Lobby> lobbies = lobbyService.listLobbies();
        
        // Map to DTOs
        return lobbies.stream()
                .map(lobby -> mapper.lobbyEntityToResponseDTO(lobby))
                .collect(Collectors.toList());
    }
}
