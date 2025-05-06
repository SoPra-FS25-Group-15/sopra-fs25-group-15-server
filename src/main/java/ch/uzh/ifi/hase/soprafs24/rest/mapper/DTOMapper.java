package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateResponseDTO;

@Component
public class DTOMapper {

    private final UserRepository userRepository;

    @Autowired
    public DTOMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 1) Registration
    public User toEntity(UserRegisterRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        
        // Default status assigned in service or here
        user.setStatus(ch.uzh.ifi.hase.soprafs24.constant.UserStatus.OFFLINE);

        // Build profile
        UserProfile profile = new UserProfile();
        profile.setUsername(dto.getUsername());
        profile.setXp(0); // default
        profile.setAchievements(new ArrayList<>());
        user.setProfile(profile);

        return user;
    }

    public UserRegisterResponseDTO toRegisterResponse(User user) {
        UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        dto.setToken(user.getToken());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // 2) Login
    public UserLoginResponseDTO toLoginResponse(User user) {
        UserLoginResponseDTO dto = new UserLoginResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setToken(user.getToken());
        dto.setPoints(user.getProfile().getXp()); // interpret "points" as xp
        return dto;
    }

    // 3) Get Current Profile (/api/auth/me)
    public UserMeDTO toUserMeDTO(User user) {
        UserMeDTO dto = new UserMeDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        dto.setToken(user.getToken());
        return dto;
    }

    // 4) Get Public Profile
    public UserPublicDTO toUserPublicDTO(User user) {
        UserPublicDTO dto = new UserPublicDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setXp(user.getProfile().getXp());
        dto.setAchievements(user.getProfile().getAchievements());
        dto.setEmail(user.getEmail());
        return dto;
    }

    // 5) Update Profile
    public void updateEntityFromDTO(User user, UserUpdateRequestDTO dto) {
        user.getProfile().setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        if(dto.getStatsPublic() != null) {
            user.getProfile().setStatsPublic(dto.getStatsPublic());
        }
    }
  
    public UserUpdateResponseDTO toUpdateResponse(User user) {
        UserUpdateResponseDTO dto = new UserUpdateResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }

    // 6) User Stats
    public UserStatsDTO toUserStatsDTO(User user) {
        UserStatsDTO dto = new UserStatsDTO();
        dto.setGamesPlayed(user.getProfile().getGamesPlayed());
        dto.setWins(user.getProfile().getWins());
        dto.setXp(user.getProfile().getXp());
        return dto;
    }

    // Enhanced friend request mapping 
    public FriendRequestDTO toFriendRequestDTO(FriendRequest request, User currentUser) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setRequestId(request.getId());
        dto.setSender(request.getSender().getId());
        dto.setSenderUsername(request.getSender().getProfile().getUsername());
        dto.setRecipient(request.getRecipient().getId());
        dto.setRecipientUsername(request.getRecipient().getProfile().getUsername());
        dto.setStatus(request.getStatus().name().toLowerCase());
        dto.setCreatedAt(request.getCreatedAt().toString());
        
        // Determine if this is an incoming request for the current user
        dto.setIncoming(request.getRecipient().getId().equals(currentUser.getId()));
        
        return dto;
    }
    
    // Basic friend request mapping (for backward compatibility)
    public FriendRequestDTO toFriendRequestDTO(FriendRequest request) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setRequestId(request.getId());
        dto.setRecipient(request.getRecipient().getId());
        dto.setAction(request.getStatus().name().toLowerCase());
        return dto;
    }

    // 8) Friend mapping
    public FriendDTO toFriendDTO(User user) {
        FriendDTO dto = new FriendDTO();
        dto.setFriendId(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        return dto;
    }

    // 9) Lobby mapping: Convert a LobbyRequestDTO into a Lobby entity.
    public Lobby lobbyRequestDTOToEntity(LobbyRequestDTO dto) {
        Lobby lobby = new Lobby();
        lobby.setPrivate(dto.isPrivate());

        if (dto.isPrivate()) {
            lobby.setMode(LobbyConstants.MODE_SOLO);
            lobby.setMaxPlayersPerTeam(1);
        } else {
            lobby.setMode(dto.getMode() != null ? dto.getMode().toLowerCase() : LobbyConstants.MODE_SOLO);
            if (LobbyConstants.MODE_TEAM.equalsIgnoreCase(lobby.getMode())) {
                lobby.setMaxPlayersPerTeam(dto.getMaxPlayersPerTeam() != null ? dto.getMaxPlayersPerTeam() : 2);
            } else {
                lobby.setMaxPlayersPerTeam(1);
            }
        }

        lobby.setMaxPlayers(dto.getMaxPlayers() != null ? dto.getMaxPlayers() : 8);
        return lobby;
    }

    // 10) Lobby mapping: Convert a Lobby entity into a LobbyResponseDTO.
    public LobbyResponseDTO lobbyEntityToResponseDTO(Lobby lobby) {
        LobbyResponseDTO dto = new LobbyResponseDTO();
        dto.setLobbyId(lobby.getId());
        dto.setMode(lobby.getMode());
        dto.setPrivate(lobby.isPrivate());
        dto.setCode(lobby.getLobbyCode());
        dto.setLobbyCode(lobby.getLobbyCode());

        if (LobbyConstants.MODE_TEAM.equalsIgnoreCase(lobby.getMode())) {
            dto.setPlayersPerTeam(lobby.getMaxPlayersPerTeam() != null ? lobby.getMaxPlayersPerTeam() : 2);
            dto.setMaxPlayers((String) null); // Explicitly set to null for team mode
        } else {
            dto.setMaxPlayers(String.valueOf(lobby.getMaxPlayers() != null ? lobby.getMaxPlayers() : 8)); // Explicitly convert to String
            dto.setPlayersPerTeam(null);
        }

        // Set roundCardsStartAmount to 2 as per requirements
        // This represents the fixed number of round cards in the RoundCardService
        dto.setRoundCardsStartAmount(2);
    

        dto.setCreatedAt(lobby.getCreatedAt());
        dto.setStatus(lobby.getStatus());

        if (LobbyConstants.MODE_TEAM.equalsIgnoreCase(lobby.getMode())) {
            if (lobby.getTeams() != null && !lobby.getTeams().isEmpty()) {
                Map<String, List<UserPublicDTO>> teamDTOs = new HashMap<>();
                lobby.getTeams().forEach((teamName, teamMembers) -> {
                    List<UserPublicDTO> memberDTOs = teamMembers.stream()
                        .map(this::toUserPublicDTO)
                        .collect(Collectors.toList());
                    teamDTOs.put(teamName, memberDTOs);
                });
                dto.setTeams(teamDTOs);
            }
        } else {
            List<UserPublicDTO> playerDTOs = lobby.getPlayers() != null
                ? lobby.getPlayers().stream().map(this::toUserPublicDTO).collect(Collectors.toList())
                : new ArrayList<>();
            dto.setPlayers(playerDTOs);
        }

        return dto;
    }
    
    // 9) Lobby Leave mapping: Create a LobbyLeaveResponseDTO from a Lobby entity and message
    public LobbyLeaveResponseDTO toLobbyLeaveResponse(Lobby lobby, String message) {
        LobbyResponseDTO lobbyResponseDTO = lobby != null ? 
            lobbyEntityToResponseDTO(lobby) : null;
        return new LobbyLeaveResponseDTO(message, lobbyResponseDTO);
    }

    // Search result mapping
    public UserSearchResponseDTO toUserSearchResponseDTO(User user) {
        UserSearchResponseDTO dto = new UserSearchResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        return dto;
    }
}