package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.JoinLobbyRequestDTO;
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
        dto.setPrivate(false); // Ranked game (public)
        // Explicitly set mode to "team"
        dto.setMode("team");
        dto.setMaxPlayersPerTeam(3);

        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);

        // Verify mapping for a team lobby
        assertEquals(false, lobby.isPrivate());
        // Since mode is team, maxPlayersPerTeam should be set from DTO
        assertEquals(3, lobby.getMaxPlayersPerTeam());
        assertEquals("team", lobby.getMode());
        // Round cards should not be set by the mapper anymore - but in Lobby they're initialized as empty list
        assertNotNull(lobby.getHintsEnabled());
        assertTrue(lobby.getHintsEnabled().isEmpty());
        assertEquals(8, lobby.getMaxPlayers());
    }

    @Test
    public void testLobbyRequestDTOToEntity_SoloMode() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setPrivate(true); // Unranked game (private)
        // Explicitly set mode to "solo"
        dto.setMode("solo");
        // Even if provided, maxPlayersPerTeam should be forced to 1 in solo mode
        dto.setMaxPlayersPerTeam(3);

        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);

        // Verify mapping for a solo lobby
        assertEquals(true, lobby.isPrivate());
        // In solo mode, maxPlayersPerTeam is always set to 1
        assertEquals(1, lobby.getMaxPlayersPerTeam());
        assertEquals("solo", lobby.getMode());
        // Round cards should not be set by the mapper anymore - but in Lobby they're initialized as empty list
        assertNotNull(lobby.getHintsEnabled());
        assertTrue(lobby.getHintsEnabled().isEmpty());
        assertEquals(8, lobby.getMaxPlayers());
    }

    @Test
    public void testLobbyEntityToResponseDTO_SoloMode_DefaultMaxPlayers() {
        // Create a dummy lobby in solo mode with no maxPlayers explicitly set.
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 300L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("solo");
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
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set createdAt via reflection", e);
        }

        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(lobby);

        // Verify default maxPlayers is "8" (as String) for solo mode
        assertEquals("8", responseDTO.getMaxPlayers());
        assertNull(responseDTO.getPlayersPerTeam()); // Ensure playersPerTeam is null for solo mode
        assertEquals("CODE1234", responseDTO.getCode());
        assertEquals(2, responseDTO.getRoundCardsStartAmount());
        assertEquals(hints, responseDTO.getRoundCards());
        assertEquals("waiting", responseDTO.getStatus());
        assertEquals(now, responseDTO.getCreatedAt());
    }

    @Test
    public void testLobbyEntityToResponseDTO_TeamMode() {
        // Create a dummy lobby in team mode.
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 400L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("team");
        lobby.setPrivate(false);
        lobby.setLobbyCode("TEAMCODE");
        lobby.setMaxPlayersPerTeam(4);
        List<String> hints = Arrays.asList("H1", "H2");
        lobby.setHintsEnabled(hints);
        lobby.setStatus("waiting");
        Instant now = Instant.now();
        try {
            Field createdAtField = Lobby.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(lobby, now);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set createdAt via reflection", e);
        }

        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(lobby);

        // Verify team mode mappings
        assertNull(responseDTO.getMaxPlayers()); // Ensure maxPlayers is null for team mode
        assertEquals(4, responseDTO.getPlayersPerTeam());
        assertEquals("team", responseDTO.getMode());
        assertEquals("TEAMCODE", responseDTO.getCode());
        assertEquals(2, responseDTO.getRoundCardsStartAmount());
        assertEquals(hints, responseDTO.getRoundCards());
        assertEquals("waiting", responseDTO.getStatus());
        assertEquals(now, responseDTO.getCreatedAt());
    }

    @Test
    public void testToLobbyLeaveResponse_WithValidLobby() {
        // Create a dummy lobby
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 500L);
        } catch(IllegalArgumentException | NullPointerException e) {
            // Handle specific exceptions
            e.printStackTrace();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("solo");
        lobby.setPrivate(true); // Private = unranked
        
        String testMessage = "Left lobby successfully.";
        
        // Convert using mapper
        LobbyLeaveResponseDTO responseDTO = mapper.toLobbyLeaveResponse(lobby, testMessage);
        
        // Verify mapping results
        assertEquals(testMessage, responseDTO.getMessage());
        assertNotNull(responseDTO.getLobby());
        assertEquals(lobby.getId(), responseDTO.getLobby().getLobbyId());
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

    @Test
    public void testLobbyRequestDTOToEntity_CasualGame() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setPrivate(true); // Unranked game (private)
        dto.setMode("team"); // This should be overridden to "solo" for casual games
        dto.setMaxPlayersPerTeam(3); // This should be set to 1 for solo mode

        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);

        // Verify mapping enforces solo mode for casual games regardless of input
        assertEquals(true, lobby.isPrivate()); // Casual games are always private
        assertEquals(LobbyConstants.MODE_SOLO, lobby.getMode()); // Mode is enforced to solo for casual
        assertNotNull(lobby.getMaxPlayersPerTeam()); // Set internally
        assertEquals(1, lobby.getMaxPlayersPerTeam()); // For solo mode, always 1
    }

    @Test
    public void testLobbyEntityToResponseDTO_SoloMode() {
        // Create a casual lobby in solo mode
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 100L);
        } catch(IllegalArgumentException | NullPointerException e) {
            // Handle specific exceptions
            e.printStackTrace();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("solo");
        lobby.setPrivate(true); // Private = unranked
        lobby.setLobbyCode("12345");
        lobby.setMaxPlayersPerTeam(1); // Internal value that shouldn't be exposed
        lobby.setMaxPlayers(8);

        LobbyResponseDTO dto = mapper.lobbyEntityToResponseDTO(lobby);
        
        // Verify solo mode mappings
        assertEquals("solo", dto.getMode());
        assertEquals("8", dto.getMaxPlayers()); // MaxPlayers is now a String
        assertEquals("12345", dto.getCode()); // Check new field name
        assertEquals("12345", dto.getLobbyCode()); // Check backward compatibility
        assertNull(dto.getPlayersPerTeam()); // MaxPlayersPerTeam renamed to playersPerTeam
    }

    @Test
    public void testLobbyRequestDTOToEntity_CasualGame_EnforcesSoloMode() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setPrivate(true); // Casual/unranked game is private
        dto.setMode("team"); // Client attempts to set team mode
        dto.setMaxPlayersPerTeam(3); // Client attempts to set team size
        
        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);
        
        // Verify mapper enforces solo mode for casual games
        assertEquals(true, lobby.isPrivate()); // Casual games are always private
        assertEquals("solo", lobby.getMode()); // Mode is enforced to solo
        assertNotNull(lobby.getMaxPlayersPerTeam()); // Should be internally set to 1
        assertEquals(1, lobby.getMaxPlayersPerTeam());
    }

    @Test
    public void testLobbyRequestDTOToEntity_RankedGame_RespectsTeamMode() {
        LobbyRequestDTO dto = new LobbyRequestDTO();
        dto.setPrivate(false); // Ranked game
        dto.setMode("team"); // Client sets team mode
        dto.setMaxPlayersPerTeam(2); // Client sets team size
        
        Lobby lobby = mapper.lobbyRequestDTOToEntity(dto);
        
        // Verify mapper respects mode for ranked games
        assertEquals(false, lobby.isPrivate()); // Ranked games are public
        assertEquals("team", lobby.getMode()); // Mode is preserved
        assertNotNull(lobby.getMaxPlayersPerTeam());
        assertEquals(2, lobby.getMaxPlayersPerTeam());
    }

    @Test
    public void testLobbyEntityToResponseDTO_SoloMode_HidesMaxPlayersPerTeam() {
        // Create a solo mode lobby
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 500L);
        } catch(IllegalArgumentException | NullPointerException e) {
            // Handle specific exceptions
            e.printStackTrace();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("solo");
        lobby.setPrivate(true);
        lobby.setLobbyCode("12345");
        lobby.setMaxPlayersPerTeam(1); // Internal value
        lobby.setMaxPlayers(8);
        
        LobbyResponseDTO dto = mapper.lobbyEntityToResponseDTO(lobby);
        
        // Verify solo mode mappings
        assertEquals("solo", dto.getMode());
        assertEquals("8", dto.getMaxPlayers()); // Now a String
        assertNull(dto.getPlayersPerTeam()); // Renamed field
        assertEquals("12345", dto.getCode()); // Check new field
    }

    @Test
    public void testLobbyEntityToResponseDTO_TeamMode_ShowsMaxPlayersPerTeam() {
        // Create a team mode lobby
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 600L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("team");
        lobby.setPrivate(false);
        lobby.setMaxPlayersPerTeam(2);
        
        LobbyResponseDTO dto = mapper.lobbyEntityToResponseDTO(lobby);
        
        // Verify team mode mappings
        assertEquals("team", dto.getMode());
        assertEquals(2, dto.getPlayersPerTeam()); // This line was failing - ensure it's correctly mapped
        assertNull(dto.getMaxPlayers()); // Should not expose maxPlayers
    }

    @Test
    public void testLobbyEntityToResponseDTO_RoundCardsStartAmount() {
        // Create a lobby with round cards/hints
        Lobby lobby = new Lobby();
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lobby, 700L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        lobby.setMode("solo");
        lobby.setPrivate(true);
        
        // Set 5 round cards/hints
        List<String> hints = Arrays.asList("C1", "C2", "C3", "C4", "C5");
        lobby.setHintsEnabled(hints);
        
        LobbyResponseDTO dto = mapper.lobbyEntityToResponseDTO(lobby);
        
        // Verify roundCardsStartAmount is set correctly
        assertEquals(5, dto.getRoundCardsStartAmount());
        // Verify backward compatibility
        assertEquals(hints, dto.getRoundCards());
    }

    @Test
    public void testJoinLobbyRequestDTO() {
        // Create a JoinLobbyRequestDTO
        JoinLobbyRequestDTO joinDTO = new JoinLobbyRequestDTO();
        joinDTO.setMode("solo");
        joinDTO.setLobbyCode("12345");
        joinDTO.setFriendInvited(true);
        
        // Verify the DTO has the expected properties
        assertEquals("solo", joinDTO.getMode());
        assertEquals("12345", joinDTO.getLobbyCode());
        assertTrue(joinDTO.isFriendInvited());
    }
}