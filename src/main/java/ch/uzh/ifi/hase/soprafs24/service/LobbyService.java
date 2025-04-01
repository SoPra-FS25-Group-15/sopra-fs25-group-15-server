package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GenericMessageResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyInviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

@Service
public class LobbyService {

    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final DTOMapper mapper;
    private final Random random = new Random();

    @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository, DTOMapper mapper) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Creates a lobby. For casual (unranked) lobbies, generates a numeric code.
     */
    @Transactional
    public Lobby createLobby(Lobby lobby) {
        if (lobby.getGameType().equalsIgnoreCase(LobbyConstants.GAME_TYPE_RANKED)) {
            lobby.setPrivate(false); // Ranked games are public by default
        } else {
            lobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE); // Use the boolean constant
            String code = generateNumericLobbyCode();
            lobby.setLobbyCode(code);
            System.out.println("Generated code for new lobby: " + code);
        }
    
        // Determine mode from client input; default is solo.
        String mode = (lobby.getMode() != null) ? lobby.getMode().toLowerCase() : LobbyConstants.MODE_SOLO;
        lobby.setMode(mode);
    
        if (mode.equals(LobbyConstants.MODE_TEAM)) {
            // For team mode, each team is limited to 2 players.
            lobby.setMaxPlayersPerTeam(2);
            // Overall lobby capacity is still 8 players.
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            // Initialize teams map.
            if (lobby.getTeams() == null) {
                lobby.setTeams(new HashMap<>());
            }
        } else {
            // Solo mode: enforce solo settings.
            lobby.setMaxPlayersPerTeam(1);
            if (lobby.getPlayers() == null) {
                lobby.setPlayers(new ArrayList<>());
            }
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            // Clear any teams information.
            lobby.setTeams(null);
        }
    
        if (lobby.getLobbyName() == null || lobby.getGameType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lobby settings");
        }
        
        return lobbyRepository.save(lobby);
    }

    /**
     * Generates a 5-digit numeric code (e.g. "12345").
     */
    private String generateNumericLobbyCode() {
        int min = 10_000;
        int max = 99_999;
        int randomNum = min + random.nextInt(max - min + 1);
        return String.valueOf(randomNum);
    }

    /**
     * Updates lobby configuration (mode, team size, round cards).
     */
    @Transactional
    public Lobby updateLobbyConfig(Long lobbyId, LobbyConfigUpdateRequestDTO config, Long requesterId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can update lobby configuration");
        }
        
        String mode = config.getMode().toLowerCase();
        if (!mode.equals(LobbyConstants.MODE_SOLO) && !mode.equals(LobbyConstants.MODE_TEAM)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode. Allowed values are 'solo' or 'team'.");
        }
        
        lobby.setMode(mode);
        
        if (mode.equals(LobbyConstants.MODE_TEAM)) {
            lobby.setMaxPlayersPerTeam(2);
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            if (lobby.getTeams() == null) {
                lobby.setTeams(new HashMap<>());
            }
            // Clear solo players list.
            lobby.setPlayers(new ArrayList<>());
        } else {
            lobby.setMaxPlayersPerTeam(1);
            if (lobby.getPlayers() == null) {
                lobby.setPlayers(new ArrayList<>());
            }
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            // Clear teams map.
            lobby.setTeams(null);
        }
        
        // Update round cards (hints)
        lobby.setHintsEnabled(config.getRoundCards());
        return lobbyRepository.save(lobby);
    }

    /**
     * Retrieves a lobby by its id.
     */
    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }

    /**
     * Lists all lobbies.
     */
    public List<Lobby> listLobbies() {
        List<Lobby> lobbies = lobbyRepository.findAll();
        if (lobbies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No available lobbies");
        }
        return lobbies;
    }

    /**
     * Invites users to a lobby (either friends or via code).
     */
    @Transactional
    public LobbyInviteResponseDTO inviteToLobby(Long lobbyId, Long hostId, InviteLobbyRequestDTO inviteRequest) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can invite players");
        }
    
        // If inviting a friend via friendId:
        if (inviteRequest.getFriendId() != null) {
            User friend = userRepository.findById(inviteRequest.getFriendId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));
            // (Optional) Verify that the friend is in the host's friend list.
            return new LobbyInviteResponseDTO(null, friend.getProfile().getUsername());
        }
    
        // Otherwise, return the numeric lobby code for non-friend invites.
        String code = lobby.getLobbyCode();
        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No code available. This might be a ranked lobby or code not generated.");
        }
        return new LobbyInviteResponseDTO(code, null);
    }

    /**
     * Deletes a lobby. Only the host can delete the lobby.
     */
    @Transactional
    public GenericMessageResponseDTO deleteLobby(Long lobbyId, Long requesterId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can delete the lobby");
        }
        lobbyRepository.delete(lobby);
        return new GenericMessageResponseDTO("Lobby disbanded successfully.");
    }

    /**
     * Join a lobby. Users can join via invitation or with a lobby code.
     */
    @Transactional
    public LobbyJoinResponseDTO joinLobby(Long lobbyId, Long userId, String team, String lobbyCode, boolean friendInvited) {
        System.out.println("Join lobby request: lobbyId=" + lobbyId + ", userId=" + userId + 
                          ", team=" + team + ", lobbyCode=" + lobbyCode + ", friendInvited=" + friendInvited);
        
        // Get lobby by ID
        Lobby lobby = getLobbyById(lobbyId);
        System.out.println("Lobby found: " + lobby.getId() + ", mode=" + lobby.getMode() + 
                          ", lobbyCode=" + lobby.getLobbyCode() + ", isPrivate=" + lobby.isPrivate());
        
        // For non-friend joins with private lobbies, validate code 
        if (!friendInvited && lobby.isPrivate()) {
            String actualCode = lobby.getLobbyCode();
            if (lobbyCode == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lobby code is required");
            }
            if (actualCode == null || !lobbyCode.equals(actualCode)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid lobby code");
            }
        }
        
        // Get user by ID
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Check if this is a team mode lobby or solo mode
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            // TEAM MODE: Add player to specific team
            
            // Validate team parameter
            if (team == null || team.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name is required for team mode");
            }
            
            // Initialize teams map if needed
            Map<String, List<User>> teams = lobby.getTeams();
            if (teams == null) {
                teams = new HashMap<>();
                lobby.setTeams(teams);
            }
            
            // Get or create the team
            List<User> teamMembers = teams.computeIfAbsent(team, k -> new ArrayList<>());
            
            // Check if team is full
            if (teamMembers.size() >= lobby.getMaxPlayersPerTeam()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team is full");
            }
            
            // Add user to team
            teamMembers.add(user);
            System.out.println("Added user " + user.getId() + " to team " + team);
        } 
        else {
            // SOLO MODE: Add player to general players list
            
            // Initialize players list if needed
            List<User> players = lobby.getPlayers();
            if (players == null) {
                players = new ArrayList<>();
                lobby.setPlayers(players);
            }
            
            // Check if the lobby is full
            Integer maxPlayers = lobby.getMaxPlayers();
            if (maxPlayers == null) {
                maxPlayers = 8; // Default value
                lobby.setMaxPlayers(maxPlayers);
            }
            
            if (players.size() >= maxPlayers) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby is full");
            }
            
            // Add user to players list if not already there
            if (!players.contains(user)) {
                players.add(user);
                System.out.println("Added user " + user.getId() + " to solo players list");
            }
        }
        
        // Save the updated lobby
        lobby = lobbyRepository.save(lobby);
        System.out.println("Saved lobby with ID: " + lobby.getId());
        
        // Create response DTO
        try {
            LobbyResponseDTO lobbyResponseDTO = mapper.lobbyEntityToResponseDTO(lobby);
            return new LobbyJoinResponseDTO("Joined lobby successfully.", lobbyResponseDTO);
        } 
        catch (Exception e) {
            System.err.println("Error converting lobby to DTO: " + e.getMessage());
            // Create a simplified response if DTO conversion fails
            LobbyResponseDTO simplifiedDTO = new LobbyResponseDTO();
            simplifiedDTO.setLobbyId(lobby.getId());
            simplifiedDTO.setLobbyName(lobby.getLobbyName());
            simplifiedDTO.setMode(lobby.getMode());
            return new LobbyJoinResponseDTO("Joined lobby successfully.", simplifiedDTO);
        }
    }
    
    /**
     * Leaves a lobby (or kicks a player if the requester is the host).
     */
    @Transactional
    public LobbyLeaveResponseDTO leaveLobby(Long lobbyId, Long requesterId, Long leavingUserId) {
        Lobby lobby = getLobbyById(lobbyId);
        
        if (!requesterId.equals(leavingUserId)) {
            // This is a kick operation
            if (!lobby.getHost().getId().equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can kick players");
            }
        }
        
        boolean removed = false;
        
        // Check if this is team mode
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            // Remove from teams
            Map<String, List<User>> teams = lobby.getTeams();
            if (teams != null) {
                for (Map.Entry<String, List<User>> teamEntry : teams.entrySet()) {
                    // Create mutable copy of the team list before modifying
                    List<User> mutableTeamList = new ArrayList<>(teamEntry.getValue());
                    boolean teamRemoved = mutableTeamList.removeIf(user -> user.getId().equals(leavingUserId));
                    if (teamRemoved) {
                        teamEntry.setValue(mutableTeamList);
                        removed = true;
                    }
                }
            }
        } else {
            // Remove from players list (solo mode)
            List<User> players = lobby.getPlayers();
            if (players != null) {
                // Create mutable copy of the players list before modifying
                List<User> mutablePlayers = new ArrayList<>(players);
                removed = mutablePlayers.removeIf(user -> user.getId().equals(leavingUserId));
                if (removed) {
                    lobby.setPlayers(mutablePlayers);
                }
            }
        }
        
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not in lobby");
        }
        
        lobby = lobbyRepository.save(lobby);
        
        try {
            return mapper.toLobbyLeaveResponse(lobby, "Left lobby successfully.");
        } catch (Exception e) {
            System.err.println("Error converting lobby to DTO: " + e.getMessage());
            // Create a simplified response if DTO conversion fails
            LobbyResponseDTO simplifiedDTO = new LobbyResponseDTO();
            simplifiedDTO.setLobbyId(lobby.getId());
            return new LobbyLeaveResponseDTO("Left lobby successfully.", simplifiedDTO);
        }
    }
}