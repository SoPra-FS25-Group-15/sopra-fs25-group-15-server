package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<String, LobbyInvite> pendingInvites = new ConcurrentHashMap<>();

    @Autowired
    public LobbyService(LobbyRepository lobbyRepository, UserRepository userRepository, DTOMapper mapper) {
        this.lobbyRepository = lobbyRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Creates a lobby. For private (unranked) lobbies, generates a numeric code.
     */
    @Transactional
    public Lobby createLobby(Lobby lobby) {
        // isPrivate flag now determines the lobby type
        // false = ranked (public lobby)
        // true = unranked (private lobby with code)
        
        if (!lobby.isPrivate()) {
            // Ranked games are public by default
            lobby.setPrivate(false);
        } else {
            // Unranked games are always private
            
            // Force solo mode for unranked games
            lobby.setMode(LobbyConstants.MODE_SOLO);
        }
        
        // Generate lobby code for all lobbies (regardless of type)
        String code = generateNumericLobbyCode();
        lobby.setLobbyCode(code);
        System.out.println("Generated code for new lobby: " + code);
    
        // If mode wasn't set above, default to solo
        if (lobby.getMode() == null) {
            lobby.setMode(LobbyConstants.MODE_SOLO);
        }
        
        String mode = lobby.getMode().toLowerCase();
    
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
            lobby.setMaxPlayersPerTeam(1); // Set internally but don't expose in UI
            if (lobby.getPlayers() == null) {
                lobby.setPlayers(new ArrayList<>());
            }
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            // Clear any teams information.
            lobby.setTeams(null);
        }
        
        // Generate default round cards instead of taking them from client
        List<String> defaultRoundCards = generateDefaultRoundCards();
        lobby.setHintsEnabled(defaultRoundCards);
        
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
     * Checks if the specified user is the host of the lobby
     * @param lobby The lobby to check
     * @param userId The ID of the user to check
     * @return true if the user is the host, false otherwise
     */
    private boolean isUserHost(Lobby lobby, Long userId) {
        if (lobby == null || userId == null || lobby.getHost() == null) {
            return false;
        }
        return lobby.getHost().getId().equals(userId);
    }
    /**
     * Updates lobby configuration (mode, team size, round cards).
     */
    @Transactional
    public Lobby updateLobbyConfig(Long lobbyId, LobbyConfigUpdateRequestDTO config, Long requesterId) {
        Lobby lobby = lobbyRepository.findById(lobbyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
        
        // Check if the requester is the host
        if (!isUserHost(lobby, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can update lobby configuration");
        }
    
        // Update mode if provided
        if (config.getMode() != null) {
            String mode = config.getMode().toLowerCase();
            if (!mode.equals(LobbyConstants.MODE_SOLO) && !mode.equals(LobbyConstants.MODE_TEAM)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Invalid mode. Allowed values are 'solo' or 'team'.");
            }
            lobby.setMode(mode);
            
            // Handle mode-specific settings but DON'T update maxPlayersPerTeam
            if (mode.equals(LobbyConstants.MODE_TEAM)) {
                // Initialize teams map if changing to team mode
                if (lobby.getTeams() == null) {
                    lobby.setTeams(new HashMap<>());
                }
                // Clear solo players list if changing to team mode
                lobby.setPlayers(null);
            } else {
                // Solo mode: initialize players list if changing to solo mode
                if (lobby.getPlayers() == null) {
                    lobby.setPlayers(new ArrayList<>());
                }
                // Clear teams if changing to solo mode
                lobby.setTeams(null);
            }
        }
        
        // Update maxPlayers if provided
        if (config.getMaxPlayers() != null) {
            if (config.getMaxPlayers() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Maximum players must be greater than 0");
            }
            // Check that the new max doesn't kick out current players
            int currentPlayerCount = getCurrentPlayerCount(lobby);
            if (config.getMaxPlayers() < currentPlayerCount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot set maximum players below the current player count (" + currentPlayerCount + ")");
            }
            lobby.setMaxPlayers(config.getMaxPlayers());
        }
        
        // Update maxPlayersPerTeam if provided (only applicable for team mode)
        if (config.getMaxPlayersPerTeam() != null && LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            if (config.getMaxPlayersPerTeam() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Maximum players per team must be greater than 0");
            }
            
            // Check if reducing maxPlayersPerTeam would kick out existing players
            if (lobby.getTeams() != null) {
                for (List<User> teamMembers : lobby.getTeams().values()) {
                    if (teamMembers.size() > config.getMaxPlayersPerTeam()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Cannot set maximum players per team below the current team size");
                    }
                }
            }
            
            lobby.setMaxPlayersPerTeam(config.getMaxPlayersPerTeam());
        }
        
        // Removed: Update roundCards if provided
        
        return lobbyRepository.save(lobby);
    }
    
    // Helper method to count current players in the lobby
    private int getCurrentPlayerCount(Lobby lobby) {
        int count = 0;
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            // Count players in teams
            for (List<User> teamMembers : lobby.getTeams().values()) {
                count += teamMembers.size();
            }
        } else {
            // Count solo players
            count = lobby.getPlayers() != null ? lobby.getPlayers().size() : 0;
        }
        return count;
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
        
        // Verify that only the host can invite players
        if (!lobby.getHost().getId().equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can invite players");
        }
    
        // If inviting a friend via friendId:
        if (inviteRequest.getFriendId() != null) {
            User friend = userRepository.findById(inviteRequest.getFriendId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));
            
            // Verify that the friend is in the host's friend list (if you have a friends system)
            // This would be the place to add the check if you have a friend system implemented
            // Example:
            // if (!isFriend(hostId, inviteRequest.getFriendId())) {
            //     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only invite friends");
            // }
            
            // Return friend invite response with username
            return new LobbyInviteResponseDTO(null, friend.getProfile().getUsername());
        }
    
        // Otherwise, return the lobby code for non-friend invites
        String code = lobby.getLobbyCode();
        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No lobby code available. This might be a code generation issue.");
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
                          ", lobbyCode=" + lobbyCode + ", friendInvited=" + friendInvited);
        
        // Get lobby by ID
        Lobby lobby = getLobbyById(lobbyId);
        System.out.println("Lobby found: " + lobby.getId() + ", mode=" + lobby.getMode() + 
                          ", lobbyCode=" + lobby.getLobbyCode() + ", isPrivate=" + lobby.isPrivate());
        
        // Get user by ID
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Validate join method:
        // 1. If joining as a friend (invited), no need to check code
        // 2. If not invited as friend, must provide correct lobby code
        if (!friendInvited) {
            // Not a friend invite, so verify lobby code
            if (lobbyCode == null || !lobbyCode.equals(lobby.getLobbyCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid lobby code");
            }
        } else {
            // Friend invite - verify that user is in host's friend list (if you have a friends system)
            // This would be the place to add the check if you have a friend system implemented
            // Example:
            // if (!isFriend(lobby.getHost().getId(), userId)) {
            //     throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only friends can join via invite");
            // }
        }
        
        // Check if this is a team mode lobby or solo mode
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            // TEAM MODE: Add player automatically to a team using fixed team names "team1" and "team2"
            Map<String, List<User>> teams = lobby.getTeams();
            if (teams == null) {
                teams = new HashMap<>();
                lobby.setTeams(teams);
            }
            String teamName = "team1";
            List<User> teamMembers = teams.get(teamName);
            if (teamMembers == null) {
                teamMembers = new ArrayList<>();
                teams.put(teamName, teamMembers);
            }
            if (teamMembers.size() >= lobby.getMaxPlayersPerTeam()) {
                teamName = "team2";
                teamMembers = teams.get(teamName);
                if (teamMembers == null) {
                    teamMembers = new ArrayList<>();
                    teams.put(teamName, teamMembers);
                }
                if (teamMembers.size() >= lobby.getMaxPlayersPerTeam()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All teams are full");
                }
            }
            teamMembers.add(user);
            System.out.println("Added user " + user.getId() + " to team " + teamName);
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
        
        // After adding the player, check if lobby is full
        boolean isLobbyFull = false;
        
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            // Check if all teams are full in team mode
            int totalTeamCapacity = 2 * lobby.getMaxPlayersPerTeam();
            int totalPlayers = lobby.getTeams().values().stream()
                .mapToInt(List::size)
                .sum();
                
            isLobbyFull = totalPlayers >= totalTeamCapacity;
        } else {
            // Check if solo lobby is full
            isLobbyFull = lobby.getPlayers().size() >= lobby.getMaxPlayers();
        }
        
        // If lobby is full, change status to in-progress
        if (isLobbyFull) {
            lobby.setStatus(LobbyConstants.LOBBY_STATUS_IN_PROGRESS);
            // Here you would also trigger any game initialization logic
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
            // Create a simplified response if DTO conversion fails - Fixed to remove lobbyName reference
            LobbyResponseDTO simplifiedDTO = new LobbyResponseDTO();
            simplifiedDTO.setLobbyId(lobby.getId());
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

    /**
     * Generates a list of default round cards to be used in the game.
     * These could be extended to include JSON mappings in the future.
     */
    private List<String> generateDefaultRoundCards() {
        // Wrap immutable list into a modifiable ArrayList to avoid UnsupportedOperationException.
        return new ArrayList<>(List.of(
            "STANDARD_CARD_1",
            "STANDARD_CARD_2",
            "STANDARD_CARD_3", 
            "STANDARD_CARD_4",
            "STANDARD_CARD_5"
        ));
    }

    /**
     * Find a lobby by its code
     */
    public Lobby getLobbyByCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby code is required");
        }
        
        Lobby lobby = lobbyRepository.findByLobbyCode(code);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        
        return lobby;
    }

    /**
     * Get current lobby for a user
     */
    public Lobby getCurrentLobbyForUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Find lobbies where this user is present
        List<Lobby> allLobbies = lobbyRepository.findAll();
        
        for (Lobby lobby : allLobbies) {
            // Check team mode lobbies
            if (LobbyConstants.MODE_TEAM.equals(lobby.getMode()) && lobby.getTeams() != null) {
                for (List<User> teamMembers : lobby.getTeams().values()) {
                    if (teamMembers.stream().anyMatch(member -> member.getId().equals(userId))) {
                        return lobby;
                    }
                }
            } 
            // Check solo mode lobbies
            else if (lobby.getPlayers() != null) {
                if (lobby.getPlayers().stream().anyMatch(player -> player.getId().equals(userId))) {
                    return lobby;
                }
            }
            
            // Also check if user is the host
            if (lobby.getHost() != null && lobby.getHost().getId().equals(userId)) {
                return lobby;
            }
        }
        
        // User is not in any lobby
        return null;
    }

    /**
     * Get pending lobby invites for a user
     */
    public List<LobbyInvite> getPendingInvitesForUser(Long userId) {
        // This is a placeholder - you would need to implement a LobbyInvite entity and repository
        // For now, we return an empty list
        return new ArrayList<>();
    }

    // Add the LobbyInvite inner class for the method above
    public static class LobbyInvite {
        private User sender;
        private String lobbyCode;
        
        public User getSender() {
            return sender;
        }
        
        public void setSender(User sender) {
            this.sender = sender;
        }
        
        public String getLobbyCode() {
            return lobbyCode;
        }
        
        public void setLobbyCode(String lobbyCode) {
            this.lobbyCode = lobbyCode;
        }
    }

    /**
     * Create a lobby invite from one user to another
     */
    public LobbyInvite createLobbyInvite(User sender, User recipient, String lobbyCode) {
        // Check if the sender actually has access to the lobby with the given code
        Lobby lobby = getLobbyByCode(lobbyCode);
        if (lobby == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
        }
        
        // Verify sender is in the lobby (host or participant)
        boolean senderInLobby = isUserInLobby(sender, lobby);
        if (!senderInLobby) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be in the lobby to send invites");
        }
        
        // Create invite
        LobbyInvite invite = new LobbyInvite();
        invite.setSender(sender);
        invite.setLobbyCode(lobbyCode);
        
        // Store the invite with a unique key
        String inviteKey = sender.getId() + "-" + recipient.getId();
        pendingInvites.put(inviteKey, invite);
        
        return invite;
    }

    /**
     * Cancel a lobby invite
     * @return true if an invite was canceled, false if no invite was found
     */
    public boolean cancelLobbyInvite(User sender, User recipient) {
        String inviteKey = sender.getId() + "-" + recipient.getId();
        LobbyInvite removed = pendingInvites.remove(inviteKey);
        return removed != null;
    }

    /**
     * Verify if a lobby invite exists with the specified details
     * 
     * @param sender The user who sent the invite
     * @param recipient The user who received the invite
     * @param lobbyCode The lobby code
     * @return true if a valid invite exists, false otherwise
     */
    public boolean verifyLobbyInvite(User sender, User recipient, String lobbyCode) {
        String inviteKey = sender.getId() + "-" + recipient.getId();
        LobbyInvite invite = pendingInvites.get(inviteKey);
        
        return invite != null && invite.getLobbyCode().equals(lobbyCode);
    }

    /**
     * Check if a user is in a lobby (either as host or participant)
     */
    private boolean isUserInLobby(User user, Lobby lobby) {
        // Check if user is host
        if (lobby.getHost().getId().equals(user.getId())) {
            return true;
        }
        
        // Check if user is in players list (solo mode)
        if (lobby.getPlayers() != null && lobby.getPlayers().stream()
                .anyMatch(player -> player.getId().equals(user.getId()))) {
            return true;
        }
        
        // Check if user is in teams (team mode)
        if (lobby.getTeams() != null) {
            for (List<User> teamMembers : lobby.getTeams().values()) {
                if (teamMembers.stream().anyMatch(member -> member.getId().equals(user.getId()))) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if a user is the host of a lobby
     *
     * @param lobbyId The lobby ID
     * @param userId The user ID
     * @return true if the user is the host, false otherwise
     */
    public boolean isUserHost(Long lobbyId, Long userId) {
        try {
            Lobby lobby = getLobbyById(lobbyId);
            return isUserHost(lobby, userId);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    /**
     * Check if a user is in a lobby (either as host or participant)
     * 
     * @param userId The user ID
     * @param lobbyId The lobby ID
     * @return true if the user is in the lobby, false otherwise
     */
    public boolean isUserInLobby(Long userId, Long lobbyId) {
        try {
            Lobby lobby = getLobbyById(lobbyId);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
            return isUserInLobby(user, lobby);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    /**
     * Check if a user has any pending invite from another user, regardless of lobby code
     * This is useful for friend invites where the lobby code might not be known
     * 
     * @param sender The user who sent the invite
     * @param recipient The user who received the invite
     * @return true if any invite exists from the sender to the recipient
     */
    public boolean hasAnyPendingInviteFrom(User sender, User recipient) {
        String inviteKey = sender.getId() + "-" + recipient.getId();
        return pendingInvites.containsKey(inviteKey);
    }
}