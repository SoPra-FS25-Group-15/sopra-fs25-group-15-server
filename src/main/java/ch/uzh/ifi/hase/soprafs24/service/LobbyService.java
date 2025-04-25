package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

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
        lobby.setPrivate(false);
        String code = generateNumericLobbyCode();
        lobby.setLobbyCode(code);
        logger.debug("Generated code for new lobby: {}", code);

        if (lobby.getMode() == null) {
            lobby.setMode(LobbyConstants.MODE_SOLO);
        }

        String mode = lobby.getMode().toLowerCase();
        if (mode.equals(LobbyConstants.MODE_TEAM)) {
            lobby.setMaxPlayersPerTeam(2);
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            if (lobby.getTeams() == null) {
                lobby.setTeams(new HashMap<>());
            }
        } else {
            lobby.setMaxPlayersPerTeam(1);
            if (lobby.getPlayers() == null) {
                lobby.setPlayers(new ArrayList<>());
            }
            if (lobby.getMaxPlayers() == null) {
                lobby.setMaxPlayers(8);
            }
            lobby.setTeams(null);
        }

        // Automatically add the host
        User host = lobby.getHost();
        if (host != null) {
            if (mode.equalsIgnoreCase(LobbyConstants.MODE_TEAM)) {
                Map<String, List<User>> teams = lobby.getTeams();
                String defaultTeam = "team1";
                teams.computeIfAbsent(defaultTeam, k -> new ArrayList<>()).add(host);
            } else {
                List<User> players = lobby.getPlayers();
                if (!players.contains(host)) {
                    players.add(host);
                }
            }
        }

        return lobbyRepository.save(lobby);
    }

    private String generateNumericLobbyCode() {
        int min = 10_000, max = 99_999;
        return String.valueOf(min + random.nextInt(max - min + 1));
    }

    @Transactional
    public Lobby updateLobbyConfig(Long lobbyId, LobbyConfigUpdateRequestDTO config, Long requesterId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can update lobby configuration");
        }

        if (config.getMode() != null) {
            String mode = config.getMode().toLowerCase();
            if (!mode.equals(LobbyConstants.MODE_SOLO) && !mode.equals(LobbyConstants.MODE_TEAM)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode. Allowed: solo or team.");
            }
            lobby.setMode(mode);
            if (mode.equals(LobbyConstants.MODE_TEAM)) {
                lobby.setTeams(new HashMap<>());
                lobby.setPlayers(null);
            } else {
                lobby.setPlayers(new ArrayList<>());
                lobby.setTeams(null);
            }
        }

        if (config.getMaxPlayers() != null) {
            if (config.getMaxPlayers() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum players must be > 0");
            }
            int currentCount = getCurrentPlayerCount(lobby);
            if (config.getMaxPlayers() < currentCount) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot set maximum below current player count (" + currentCount + ")");
            }
            lobby.setMaxPlayers(config.getMaxPlayers());
        }

        if (config.getMaxPlayersPerTeam() != null && LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            if (config.getMaxPlayersPerTeam() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max players per team must be > 0");
            }
            if (lobby.getTeams() != null) {
                for (List<User> teamMembers : lobby.getTeams().values()) {
                    if (teamMembers.size() > config.getMaxPlayersPerTeam()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cannot set max players per team below current team size");
                    }
                }
            }
            lobby.setMaxPlayersPerTeam(config.getMaxPlayersPerTeam());
        }

        return lobbyRepository.save(lobby);
    }

    /**
     * Updates the status of a lobby
     * @param lobbyId ID of the lobby
     * @param status New status for the lobby
     * @return The updated lobby
     */
    @Transactional
    public Lobby updateLobbyStatus(Long lobbyId, String status) {
        Lobby lobby = getLobbyById(lobbyId);
        lobby.setStatus(status);
        lobby = lobbyRepository.save(lobby);
        logger.info("Updated lobby {} status to {}", lobbyId, status);
        return lobby;
    }

    /**
     * Join a lobby; enforces overall maxPlayers before admitting a new user.
     */
    @Transactional
    public LobbyJoinResponseDTO joinLobby(Long lobbyId, Long userId, String team, String lobbyCode, boolean friendInvited) {
        logger.info("Join lobby request: lobbyId={}, userId={}, code={}, friendInvited={}",
                    lobbyId, userId, lobbyCode, friendInvited);

        Lobby lobby = getLobbyById(lobbyId);
        logger.debug("Lobby found: id={}, mode={}, lobbyCode={}, isPrivate={}",
                     lobby.getId(), lobby.getMode(), lobby.getLobbyCode(), lobby.isPrivate());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!friendInvited) {
            if (lobbyCode == null || !lobbyCode.equals(lobby.getLobbyCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid lobby code");
            }
        }

        // Enforce overall lobby capacity
        int currentCount = getCurrentPlayerCount(lobby);
        Integer maxPlayers = lobby.getMaxPlayers();
        if (maxPlayers == null) {
            maxPlayers = 8;
            lobby.setMaxPlayers(maxPlayers);
        }
        logger.debug("Current player count: {}, maxPlayers allowed: {}", currentCount, maxPlayers);
        if (currentCount >= maxPlayers) {
            logger.warn("Lobby {} is full ({} / {})", lobbyId, currentCount, maxPlayers);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby is full");
        }

        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            Map<String, List<User>> teams = lobby.getTeams();
            if (teams == null) {
                teams = new HashMap<>();
                lobby.setTeams(teams);
            }
            // Try to add to team1, else team2
            List<User> team1 = teams.computeIfAbsent("team1", k -> new ArrayList<>());
            if (team1.size() < lobby.getMaxPlayersPerTeam()) {
                team1.add(user);
                logger.info("Added user {} to team1", userId);
            } else {
                List<User> team2 = teams.computeIfAbsent("team2", k -> new ArrayList<>());
                if (team2.size() < lobby.getMaxPlayersPerTeam()) {
                    team2.add(user);
                    logger.info("Added user {} to team2", userId);
                } else {
                    logger.warn("All teams are full in lobby {}", lobbyId);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All teams are full");
                }
            }
        } else {
            List<User> players = lobby.getPlayers();
            if (players == null) {
                players = new ArrayList<>();
                lobby.setPlayers(players);
            }
            if (!players.contains(user)) {
                players.add(user);
                logger.info("Added user {} to solo players list", userId);
            }
        }

        // If now full, mark in-progress
        boolean nowFull = LobbyConstants.MODE_TEAM.equals(lobby.getMode())
            ? getCurrentPlayerCount(lobby) >= lobby.getMaxPlayers()
            : lobby.getPlayers().size() >= lobby.getMaxPlayers();
        if (nowFull) {
            lobby.setStatus(LobbyConstants.LOBBY_STATUS_IN_PROGRESS);
        }

        lobby = lobbyRepository.save(lobby);
        logger.debug("Saved lobby {} after join", lobby.getId());

        // Eagerly initialize before mapping
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode()) && lobby.getTeams() != null) {
            for (List<User> teamMembers : lobby.getTeams().values()) {
                for (User p : teamMembers) {
                    if (p.getProfile() != null && p.getProfile().getAchievements() != null) {
                        Hibernate.initialize(p.getProfile().getAchievements());
                    }
                }
            }
        } else if (lobby.getPlayers() != null) {
            for (User p : lobby.getPlayers()) {
                if (p.getProfile() != null && p.getProfile().getAchievements() != null) {
                    Hibernate.initialize(p.getProfile().getAchievements());
                }
            }
        }

        LobbyResponseDTO dto = mapper.lobbyEntityToResponseDTO(lobby);
        return new LobbyJoinResponseDTO("Joined lobby successfully.", dto);
    }

    /**
     * Leaves a lobby (or kicks a player if requester is host).
     */
    @Transactional
    public LobbyLeaveResponseDTO leaveLobby(Long lobbyId, Long requesterId, Long leavingUserId) {
        Lobby lobby = getLobbyById(lobbyId);
        boolean isKick = !requesterId.equals(leavingUserId);
        if (isKick && !lobby.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can kick players");
        }

        boolean removed = false;
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode())) {
            Map<String, List<User>> teams = lobby.getTeams();
            if (teams != null) {
                for (Map.Entry<String, List<User>> e : teams.entrySet()) {
                    List<User> copy = new ArrayList<>(e.getValue());
                    if (copy.removeIf(u -> u.getId().equals(leavingUserId))) {
                        e.setValue(copy);
                        removed = true;
                    }
                }
            }
        } else {
            List<User> players = lobby.getPlayers();
            if (players != null) {
                List<User> copy = new ArrayList<>(players);
                removed = copy.removeIf(u -> u.getId().equals(leavingUserId));
                if (removed) {
                    lobby.setPlayers(copy);
                }
            }
        }
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not in lobby");
        }

        lobby = lobbyRepository.save(lobby);
        return mapper.toLobbyLeaveResponse(lobby, "Left lobby successfully.");
    }

    private int getCurrentPlayerCount(Lobby lobby) {
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode()) && lobby.getTeams() != null) {
            return lobby.getTeams().values().stream().mapToInt(List::size).sum();
        } else {
            return lobby.getPlayers() != null ? lobby.getPlayers().size() : 0;
        }
    }

    public Lobby getLobbyById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));
    }

    public List<Lobby> listLobbies() {
        List<Lobby> lobbies = lobbyRepository.findAll();
        if (lobbies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No available lobbies");
        }
        return lobbies;
    }

    @Transactional
    public LobbyInviteResponseDTO inviteToLobby(Long lobbyId, Long hostId, InviteLobbyRequestDTO req) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(hostId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can invite players");
        }
        if (req.getFriendId() != null) {
            User friend = userRepository.findById(req.getFriendId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));
            return new LobbyInviteResponseDTO(null, friend.getProfile().getUsername());
        }
        String code = lobby.getLobbyCode();
        if (code == null || code.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No lobby code available");
        }
        return new LobbyInviteResponseDTO(code, null);
    }

    @Transactional
    public GenericMessageResponseDTO deleteLobby(Long lobbyId, Long requesterId) {
        Lobby lobby = getLobbyById(lobbyId);
        if (!lobby.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can delete the lobby");
        }
        lobbyRepository.delete(lobby);
        return new GenericMessageResponseDTO("Lobby disbanded successfully.");
    }

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

    @Transactional(readOnly = true)
    public Lobby getCurrentLobbyForUser(Long userId) {
        User u = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        for (Lobby lobby : lobbyRepository.findAll()) {
            if (LobbyConstants.MODE_TEAM.equals(lobby.getMode()) && lobby.getTeams() != null) {
                for (User p : lobby.getTeams().values().stream().flatMap(List::stream).toList()) {
                    if (p.getId().equals(userId)) return lobby;
                }
            } else if (lobby.getPlayers() != null && lobby.getPlayers().stream()
                       .anyMatch(p -> p.getId().equals(userId))) {
                return lobby;
            }
            if (lobby.getHost() != null && lobby.getHost().getId().equals(userId)) {
                return lobby;
            }
        }
        return null;
    }

    public List<LobbyInvite> getPendingInvitesForUser(Long userId) {
        return new ArrayList<>();
    }

    public static class LobbyInvite {
        private User sender;
        private String lobbyCode;
        public User getSender() { return sender; }
        public void setSender(User sender) { this.sender = sender; }
        public String getLobbyCode() { return lobbyCode; }
        public void setLobbyCode(String lobbyCode) { this.lobbyCode = lobbyCode; }
    }

    @Transactional
    public LobbyInvite createLobbyInvite(User sender, User recipient, String lobbyCode) {
        Lobby lobby = getLobbyByCode(lobbyCode);
        if (LobbyConstants.MODE_TEAM.equalsIgnoreCase(lobby.getMode()) && lobby.getTeams() != null) {
            Hibernate.initialize(lobby.getTeams());
            lobby.getTeams().values().forEach(Hibernate::initialize);
        } else if (lobby.getPlayers() != null) {
            Hibernate.initialize(lobby.getPlayers());
        }
        if (!isUserInLobby(sender, lobby)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be in the lobby to send invites");
        }
        LobbyInvite invite = new LobbyInvite();
        invite.setSender(sender);
        invite.setLobbyCode(lobbyCode);
        pendingInvites.put(sender.getId() + "-" + recipient.getId(), invite);
        return invite;
    }

    @Transactional
    public boolean cancelLobbyInvite(User sender, User recipient) {
        return pendingInvites.remove(sender.getId() + "-" + recipient.getId()) != null;
    }

    @Transactional
    public boolean verifyLobbyInvite(User sender, User recipient, String lobbyCode) {
        LobbyInvite invite = pendingInvites.get(sender.getId() + "-" + recipient.getId());
        return invite != null && invite.getLobbyCode().equals(lobbyCode);
    }

    private boolean isUserInLobby(User user, Lobby lobby) {
        if (lobby.getHost().getId().equals(user.getId())) return true;
        if (lobby.getPlayers() != null && lobby.getPlayers().stream()
                .anyMatch(p -> p.getId().equals(user.getId()))) return true;
        if (lobby.getTeams() != null) {
            for (List<User> team : lobby.getTeams().values()) {
                if (team.stream().anyMatch(p -> p.getId().equals(user.getId()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isUserHost(Long lobbyId, Long userId) {
        try {
            return getLobbyById(lobbyId).getHost().getId().equals(userId);
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    public boolean isUserInLobby(Long userId, Long lobbyId) {
        try {
            return isUserInLobby(userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")),
                getLobbyById(lobbyId));
        } catch (ResponseStatusException e) {
            return false;
        }
    }

    @Transactional
    public boolean hasAnyPendingInviteFrom(User sender, User recipient) {
        return pendingInvites.containsKey(sender.getId() + "-" + recipient.getId());
    }

    /**
     * Get all player IDs in a lobby
     * @param lobbyId lobby ID
     * @return list of player IDs
     */
    @Transactional(readOnly = true)
    public List<Long> getLobbyPlayerIds(Long lobbyId) {
        Lobby lobby = getLobbyById(lobbyId);
        List<Long> playerIds = new ArrayList<>();
        
        // Add the host
        if (lobby.getHost() != null) {
            playerIds.add(lobby.getHost().getId());
        }
        
        // Add all players from teams or player list
        if (LobbyConstants.MODE_TEAM.equals(lobby.getMode()) && lobby.getTeams() != null) {
            for (List<User> team : lobby.getTeams().values()) {
                for (User player : team) {
                    if (!playerIds.contains(player.getId())) {
                        playerIds.add(player.getId());
                    }
                }
            }
        } else if (lobby.getPlayers() != null) {
            // Initialize the lazy collection
            Hibernate.initialize(lobby.getPlayers());
            for (User player : lobby.getPlayers()) {
                if (!playerIds.contains(player.getId())) {
                    playerIds.add(player.getId());
                }
            }
        }
        
        logger.info("Found {} players in lobby {}: {}", playerIds.size(), lobbyId, playerIds);
        return playerIds;
    }

    /**
     * Get player tokens for a lobby
     * @param lobbyId Lobby ID
     * @return List of player tokens in the lobby
     */
    public List<String> getLobbyPlayerTokens(Long lobbyId) {
        Lobby lobby = getLobbyById(lobbyId);
        
        if (lobby.getMode().equals(LobbyConstants.MODE_TEAM)) {
            // For team mode, collect tokens from all teams
            return lobby.getTeams().values().stream()
                .flatMap(List::stream)
                .map(User::getToken)
                .collect(Collectors.toList());
        } else {
            // For solo mode, get tokens from players list
            return lobby.getPlayers().stream()
                .map(User::getToken)
                .collect(Collectors.toList());
        }
    }

    /**
     * Check if a user with the given token is the host of a lobby
     * @param lobbyId Lobby ID
     * @param token User token
     * @return true if the user is the host
     */
    public boolean isUserHostByToken(Long lobbyId, String token) {
        Lobby lobby = getLobbyById(lobbyId);
        
        if (lobby == null || lobby.getHost() == null) {
            return false;
        }
        
        return lobby.getHost().getToken().equals(token);
    }

    @Transactional
  public void handleUserDisconnect(Long userId) {
    logger.info("Handling WebSocket disconnect for user {}", userId);

    // Eagerly initialize so we can remove players/teams without lazy errors
    List<Lobby> userLobbies = lobbyRepository.findAll().stream()
      .peek(lobby -> {
        // Load whichever collection is in play
        if (lobby.getPlayers() != null) {
          Hibernate.initialize(lobby.getPlayers());
        }
        if (lobby.getTeams() != null) {
          Hibernate.initialize(lobby.getTeams());
          lobby.getTeams().values().forEach(Hibernate::initialize);
        }
      })
      .filter(lobby -> isUserInLobby(userId, lobby.getId()))
      .collect(Collectors.toList());

    if (userLobbies.isEmpty()) {
      logger.debug("User {} was not in any lobbies", userId);
      return;
    }

    for (Lobby lobby : userLobbies) {
      try {
        logger.info("Processing user {} disconnect from lobby {}", userId, lobby.getId());
        if (isUserHost(lobby.getId(), userId)) {
          logger.info("User {} was host of lobby {} - deleting lobby", userId, lobby.getId());
          deleteLobby(lobby.getId(), userId);
        } else {
          logger.info("User {} was player in lobby {} - removing from lobby", userId, lobby.getId());
          leaveLobby(lobby.getId(), userId, userId);
        }
      } catch (Exception e) {
        logger.error("Error processing disconnect for user {} from lobby {}: {}", 
                     userId, lobby.getId(), e.getMessage(), e);
      }
    }
  }
  
}
