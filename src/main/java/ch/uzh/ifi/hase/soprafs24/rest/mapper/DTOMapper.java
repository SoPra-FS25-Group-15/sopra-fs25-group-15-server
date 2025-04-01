package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterResponseDTO;
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
        profile.setMmr(0); // default
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
        dto.setPoints(user.getProfile().getMmr()); // interpret "points" as mmr
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
        dto.setMmr(user.getProfile().getMmr());
        dto.setAchievements(user.getProfile().getAchievements());
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
        dto.setMmr(user.getProfile().getMmr());
        return dto;
    }

    // 7) Lobby mapping: Convert a LobbyRequestDTO into a Lobby entity.
    public Lobby lobbyRequestDTOToEntity(LobbyRequestDTO dto) {
        Lobby lobby = new Lobby();
        lobby.setLobbyName(dto.getLobbyName());
        lobby.setGameType(dto.getGameType());
        
        // For ranked mode, set to public (isPrivate=false); for casual, use the constant value
        if(dto.getGameType().equalsIgnoreCase("ranked")) {
            lobby.setPrivate(false);
        } else {
            lobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE);
        }
        
        // Use the mode from the DTO if provided; otherwise default to solo.
        if(dto.getMode() != null && !dto.getMode().isEmpty()) {
            lobby.setMode(dto.getMode().toLowerCase());
        } else {
            lobby.setMode(LobbyConstants.MODE_SOLO);
        }
        // Set team-related configuration only if the lobby is in team mode.
        if(LobbyConstants.MODE_TEAM.equalsIgnoreCase(lobby.getMode())) {
            lobby.setMaxPlayersPerTeam(dto.getMaxPlayersPerTeam());
        } else {
            // Ensure that for solo mode maxPlayersPerTeam is explicitly cleared
            lobby.setMaxPlayersPerTeam(null);
        }
        // Set hints.
        lobby.setHintsEnabled(dto.getHintsEnabled());
        return lobby;
    }

    // 8) Lobby mapping: Convert a Lobby entity into a LobbyResponseDTO.
    public LobbyResponseDTO lobbyEntityToResponseDTO(Lobby lobby) {
        LobbyResponseDTO dto = new LobbyResponseDTO();
        dto.setLobbyId(lobby.getId());
        dto.setLobbyName(lobby.getLobbyName());
        // Send back the actual mode of the lobby ("solo" or "team").
        dto.setMode(lobby.getMode());
        dto.setGameType(lobby.getGameType());
        dto.setPrivate(lobby.isPrivate());
        dto.setLobbyCode(lobby.getLobbyCode());
        // In solo mode, teams are not used; for free-for-all default to 8 players.
        if(LobbyConstants.MODE_SOLO.equalsIgnoreCase(lobby.getMode())) {
            dto.setMaxPlayers(lobby.getMaxPlayers() != null ? lobby.getMaxPlayers() : 8);
        }
        dto.setRoundCards(lobby.getHintsEnabled());
        dto.setCreatedAt(lobby.getCreatedAt());
        dto.setStatus(lobby.getStatus());
        
        // Map players.
        List<UserPublicDTO> playerDTOs = lobby.getPlayers() != null
            ? lobby.getPlayers().stream().map(this::toUserPublicDTO).collect(Collectors.toList())
            : new ArrayList<>();
        dto.setPlayers(playerDTOs);
        
        return dto;
    }
}