package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
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

public class DTOMapperTest {

    private DTOMapper mapper;
    private User dummyUser;

    @BeforeEach
    public void setup() {
        UserRepository userRepository = mock(UserRepository.class);
        mapper = new DTOMapper(userRepository);
        dummyUser = new User();

        // Use reflection to set the ID field directly for dummyUser
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dummyUser, 100L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }

        dummyUser.setEmail("user@example.com");
        dummyUser.setPassword("secret");
        dummyUser.setToken("dummy-token");
        dummyUser.setStatus(UserStatus.OFFLINE);
        dummyUser.setCreatedAt(Instant.now());

        UserProfile profile = new UserProfile();
        profile.setUsername("dummyUser");
        profile.setMmr(1500);
        profile.setAchievements(Arrays.asList("First Win", "Sharp Shooter"));
        profile.setFriends(new ArrayList<>());
        profile.setStatsPublic(true);
        dummyUser.setProfile(profile);
    }

    @Test
    public void testToEntity_fromUserRegisterRequestDTO() {
        UserRegisterRequestDTO registerDTO = new UserRegisterRequestDTO();
        registerDTO.setUsername("newUser");
        registerDTO.setEmail("new@example.com");
        registerDTO.setPassword("newSecret");

        User newUser = mapper.toEntity(registerDTO);

        assertEquals(registerDTO.getUsername(), newUser.getProfile().getUsername());
        assertEquals(registerDTO.getEmail(), newUser.getEmail());
        assertEquals(registerDTO.getPassword(), newUser.getPassword());
        // defaults
        assertEquals(0, newUser.getProfile().getMmr());
        assertNotNull(newUser.getProfile().getAchievements());
    }

    @Test
    public void testToRegisterResponse() {
        UserRegisterResponseDTO responseDTO = mapper.toRegisterResponse(dummyUser);

        assertEquals(dummyUser.getId(), responseDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), responseDTO.getUsername());
        assertEquals(dummyUser.getEmail(), responseDTO.getEmail());
        assertEquals(dummyUser.getToken(), responseDTO.getToken());
        assertEquals(dummyUser.getCreatedAt(), responseDTO.getCreatedAt());
    }

    @Test
    public void testToLoginResponse() {
        UserLoginResponseDTO loginDTO = mapper.toLoginResponse(dummyUser);

        assertEquals(dummyUser.getId(), loginDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), loginDTO.getUsername());
        assertEquals(dummyUser.getToken(), loginDTO.getToken());
        // points interpreted as mmr
        assertEquals(dummyUser.getProfile().getMmr(), loginDTO.getPoints());
    }

    @Test
    public void testToUserMeDTO() {
        UserMeDTO meDTO = mapper.toUserMeDTO(dummyUser);

        assertEquals(dummyUser.getId(), meDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), meDTO.getUsername());
        assertEquals(dummyUser.getEmail(), meDTO.getEmail());
        assertEquals(dummyUser.getToken(), meDTO.getToken());
    }

    @Test
    public void testToUserPublicDTO() {
        UserPublicDTO publicDTO = mapper.toUserPublicDTO(dummyUser);

        assertEquals(dummyUser.getId(), publicDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), publicDTO.getUsername());
        assertEquals(dummyUser.getProfile().getMmr(), publicDTO.getMmr());
        assertEquals(dummyUser.getProfile().getAchievements(), publicDTO.getAchievements());
    }

    @Test
    public void testUpdateEntityFromDTO() {
        UserUpdateRequestDTO updateDTO = new UserUpdateRequestDTO();
        updateDTO.setUsername("updatedUser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setStatsPublic(false);

        mapper.updateEntityFromDTO(dummyUser, updateDTO);

        assertEquals("updatedUser", dummyUser.getProfile().getUsername());
        assertEquals("updated@example.com", dummyUser.getEmail());
        assertEquals(false, dummyUser.getProfile().isStatsPublic());
    }

    @Test
    public void testToUpdateResponse() {
        UserUpdateResponseDTO updateResp = mapper.toUpdateResponse(dummyUser);

        assertEquals(dummyUser.getId(), updateResp.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), updateResp.getUsername());
        assertEquals(dummyUser.getEmail(), updateResp.getEmail());
    }

    @Test
    public void testToUserStatsDTO() {
        dummyUser.getProfile().setGamesPlayed(20);
        dummyUser.getProfile().setWins(12);
        dummyUser.getProfile().setMmr(1550);

        UserStatsDTO statsDTO = mapper.toUserStatsDTO(dummyUser);

        assertEquals(20, statsDTO.getGamesPlayed());
        assertEquals(12, statsDTO.getWins());
        assertEquals(1550, statsDTO.getMmr());
    }
    // New tests for Lobby DTO mappings to comply with solo/team modes

    @Test
    public void testLobbyRequestDTOToEntity_TeamMode() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setLobbyName("Team Lobby");
        dto.setGameType("unranked");
        // Explicitly set mode to "team"
        dto.setMode("team");
        dto.setMaxPlayersPerTeam(3);
        List<String> hints = Arrays.asList("Hint1", "Hint2");
        dto.setHintsEnabled(hints);

        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);

        // Verify mapping for a team lobby
        assertEquals("Team Lobby", lobby.getLobbyName());
        assertEquals("unranked", lobby.getGameType());
        // For casual play, lobbyType forced to "private"
        assertEquals(LobbyConstants.IS_LOBBY_PRIVATE, lobby.isPrivate());
        // Since mode is team, maxPlayersPerTeam should be set from DTO
        assertEquals(3, lobby.getMaxPlayersPerTeam());
        assertEquals("team", lobby.getMode());
        assertEquals(hints, lobby.getHintsEnabled());
    }
    @Test
    public void testLobbyRequestDTOToEntity_SoloMode() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setLobbyName("Solo Lobby");
        dto.setGameType("unranked");
        // Explicitly set mode to "solo"
        dto.setMode("solo");
        // Even if provided, maxPlayersPerTeam should be ignored in solo mode
        dto.setMaxPlayersPerTeam(3);
        List<String> hints = Arrays.asList("HintA", "HintB");
        dto.setHintsEnabled(hints);
    
        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);
    
        // Verify mapping for a solo lobby
        assertEquals("Solo Lobby", lobby.getLobbyName());
        assertEquals("unranked", lobby.getGameType());
        assertEquals(LobbyConstants.IS_LOBBY_PRIVATE, lobby.isPrivate());
        // In solo mode, team-related configuration is not set.
        assertNull(lobby.getMaxPlayersPerTeam());
        assertEquals("solo", lobby.getMode());
        assertEquals(hints, lobby.getHintsEnabled());
    }

    @Test
    public void testLobbyEntityToResponseDTO_SoloMode_DefaultMaxPlayers() {
        // Create a dummy lobby in solo mode with no maxPlayers explicitly set.
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 300L);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        lobby.setLobbyName("Solo Response");
        lobby.setMode("solo");
        lobby.setGameType("unranked");
        lobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE);
        lobby.setLobbyCode("CODE1234");
        // Do not set maxPlayers for solo mode: should default to 8 in the DTO.
        List<String> hints = Arrays.asList("R1", "R2");
        lobby.setHintsEnabled(hints);
        lobby.setStatus("waiting");
        Instant now = Instant.now();
        try {
            Field createdAtField = Lobby.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(lobby, now);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(lobby);
        // Since mode is solo and lobby.getMaxPlayers() is null, default maxPlayers should be 8.
        assertEquals(8, responseDTO.getMaxPlayers());
    }

    @Test
    public void testLobbyEntityToResponseDTO_TeamMode() {
        // Create a dummy lobby in team mode.
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 400L);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        lobby.setLobbyName("Team Response");
        lobby.setMode("team");
        lobby.setGameType("unranked");
        lobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE);
        lobby.setLobbyCode("TEAMCODE");
        // For team mode, maxPlayers is not used in the response DTO.
        lobby.setMaxPlayersPerTeam(4);
        List<String> hints = Arrays.asList("H1", "H2");
        lobby.setHintsEnabled(hints);
        lobby.setStatus("waiting");
        Instant now = Instant.now();
        try {
            Field createdAtField = Lobby.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(lobby, now);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(lobby);
        // For team mode, maxPlayers is not set in the DTO (only for solo mode).
        assertEquals(null, responseDTO.getMaxPlayers());
        assertEquals("team", responseDTO.getMode());
        assertEquals("TEAMCODE", responseDTO.getLobbyCode());
        assertEquals("waiting", responseDTO.getStatus());
    }
        // ...existing code...

    @Test
    public void testToLobbyLeaveResponse_WithValidLobby() {
        // Create a dummy lobby
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 500L);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        lobby.setLobbyName("Test Lobby");
        lobby.setMode("solo");
        lobby.setGameType("unranked");
        
        String testMessage = "Left lobby successfully.";
        
        // Convert using mapper
        LobbyLeaveResponseDTO responseDTO = mapper.toLobbyLeaveResponse(lobby, testMessage);
        
        // Verify mapping results
        assertEquals(testMessage, responseDTO.getMessage());
        assertNotNull(responseDTO.getLobby());
        assertEquals(lobby.getId(), responseDTO.getLobby().getLobbyId());
        assertEquals(lobby.getLobbyName(), responseDTO.getLobby().getLobbyName());
        assertEquals(lobby.getMode(), responseDTO.getLobby().getMode());
    }
    
    @Test
    public void testToLobbyLeaveResponse_WithNullLobby() {
        String testMessage = "Lobby was disbanded.";
        
        // Test with null lobby
        LobbyLeaveResponseDTO responseDTO = mapper.toLobbyLeaveResponse(null, testMessage);
        
        // Verify mapping results
        assertEquals(testMessage, responseDTO.getMessage());
        assertNull(responseDTO.getLobby());
    }
}