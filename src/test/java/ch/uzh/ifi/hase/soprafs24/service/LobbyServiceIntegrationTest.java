package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyInviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

@WebAppConfiguration
@SpringBootTest
public class LobbyServiceIntegrationTest {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DTOMapper mapper;

    private User hostUser;
    private Lobby lobby;

    @BeforeEach
    public void setup() {
        // Clear repositories before each test.
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
    
        // Create and persist a host user.
        hostUser = new User();
        hostUser.setEmail("host@example.com");
        hostUser.setPassword("password");
        hostUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the host user.
        UserProfile hostProfile = new UserProfile();
        hostProfile.setUsername("HostUser");
        hostProfile.setMmr(0); // Default MMR
        hostProfile.setAchievements(new ArrayList<>()); // Empty achievements list
        hostUser.setProfile(hostProfile);
        
        hostUser = userRepository.save(hostUser); // persisted hostUser
    
        // Create a lobby in team mode and assign the host.
        lobby = new Lobby();
        lobby.setLobbyName("Test Lobby");
        lobby.setGameType(LobbyConstants.GAME_TYPE_UNRANKED);
        // For team mode, maxPlayersPerTeam is used.
        lobby.setMaxPlayersPerTeam(2);
        lobby.setHintsEnabled(List.of("Hint1", "Hint2"));
        lobby.setHost(hostUser);
        // Explicitly setting mode to team.
        lobby.setMode(LobbyConstants.MODE_TEAM);
        lobby = lobbyService.createLobby(lobby);
    }

    @Test
    public void testCreateLobby_success() {
        // The lobby created in setup should have generated fields.
        assertNotNull(lobby.getId());
        assertEquals(LobbyConstants.IS_LOBBY_PRIVATE, lobby.isPrivate());
        assertNotNull(lobby.getLobbyCode());
        assertEquals(LobbyConstants.MODE_TEAM, lobby.getMode());
        assertEquals(LobbyConstants.LOBBY_STATUS_WAITING, lobby.getStatus());
        assertNotNull(lobby.getCreatedAt());
    }

    @Test
    public void testUpdateLobbyConfig_success() {
        LobbyConfigUpdateRequestDTO configUpdate = new LobbyConfigUpdateRequestDTO();
        configUpdate.setMode(LobbyConstants.MODE_TEAM);
        configUpdate.setMaxPlayersPerTeam(2);
        List<String> roundCards = new ArrayList<>();
        roundCards.add("card1");
        roundCards.add("card2");
        configUpdate.setRoundCards(roundCards);
    
        Lobby updatedLobby = lobbyService.updateLobbyConfig(lobby.getId(), configUpdate, hostUser.getId());
        assertEquals(LobbyConstants.MODE_TEAM, updatedLobby.getMode());
        assertEquals(2, updatedLobby.getMaxPlayersPerTeam());
        assertEquals(List.of("card1", "card2"), updatedLobby.getHintsEnabled());
    }

    @Test
    public void testInviteToLobby_successNonFriend() {
        InviteLobbyRequestDTO inviteRequest = new InviteLobbyRequestDTO();
        inviteRequest.setLobbyCode(null); // Indicates non-friend invite
    
        LobbyInviteResponseDTO inviteDTO = lobbyService.inviteToLobby(lobby.getId(), hostUser.getId(), inviteRequest);
        assertNotNull(inviteDTO.getLobbyCode());
        assertNull(inviteDTO.getInvitedFriend());
    }
    
    @Test
    public void testInviteToLobby_failNotHost() {
        // Create and persist another user that's not the host.
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser.setStatus(UserStatus.OFFLINE);
        User savedOtherUser = userRepository.save(otherUser);
    
        InviteLobbyRequestDTO inviteRequest = new InviteLobbyRequestDTO();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
            lobbyService.inviteToLobby(lobby.getId(), savedOtherUser.getId(), inviteRequest)
        );
        assertTrue(exception.getMessage().contains("Only the host can invite players"));
    }
    
    @Test
    public void testJoinLobby_success_teamMode() {
        // Arrange: Create and persist a join user.
        User joinUser = new User();
        joinUser.setEmail("join@example.com");
        joinUser.setPassword("password");
        joinUser.setStatus(UserStatus.OFFLINE);
    
        UserProfile profile = new UserProfile();
        profile.setUsername("JoinUser");
        profile.setMmr(0); // Default MMR
        profile.setAchievements(new ArrayList<>());
        joinUser.setProfile(profile);
        User savedJoinUser = userRepository.save(joinUser);
    
        // For team mode join, provide team name "blue"
        LobbyJoinResponseDTO joinResponse = lobbyService.joinLobby(
            lobby.getId(),
            savedJoinUser.getId(),
            "blue",
            lobby.getLobbyCode(),
            false
        );
    
        assertNotNull(joinResponse);
        assertEquals("Joined lobby successfully.", joinResponse.getMessage());
    }
    
    @Test
    public void testCreateAndJoinLobby_soloMode() {
        // Create a solo lobby by setting mode to solo.
        // In solo mode, even if a maxPlayersPerTeam value is provided,
        // the mapper/service should clear it so that the response defaults to 8.
        Lobby soloLobby = new Lobby();
        soloLobby.setLobbyName("Solo Lobby");
        soloLobby.setGameType(LobbyConstants.GAME_TYPE_UNRANKED);
        soloLobby.setMaxPlayersPerTeam(3); // Provided value that should be ignored.
        soloLobby.setHintsEnabled(List.of("SoloHint1", "SoloHint2"));
        soloLobby.setHost(hostUser);
        soloLobby.setMode(LobbyConstants.MODE_SOLO);
        soloLobby = lobbyService.createLobby(soloLobby);
    
        // Validate solo lobby creation.
        assertNotNull(soloLobby.getId()); 
        assertEquals(LobbyConstants.IS_LOBBY_PRIVATE, soloLobby.isPrivate());
        assertEquals(LobbyConstants.MODE_SOLO, soloLobby.getMode());
        // Instead of asserting the cleared entity field, verify via DTO conversion.
        LobbyResponseDTO soloResponseDTO = mapper.lobbyEntityToResponseDTO(soloLobby);
        // For solo mode, if maxPlayersPerTeam is null, the DTO defaults it to 8.
        assertEquals(8, soloResponseDTO.getMaxPlayers());
    
        // Test joining the solo lobby.
        User joinSoloUser = new User();
        joinSoloUser.setEmail("solojoin@example.com");
        joinSoloUser.setPassword("password");
        joinSoloUser.setStatus(UserStatus.OFFLINE);
        UserProfile joinSoloProfile = new UserProfile();
        joinSoloProfile.setUsername("SoloJoinUser");
        joinSoloProfile.setMmr(0);
        joinSoloProfile.setAchievements(new ArrayList<>());
        joinSoloUser.setProfile(joinSoloProfile);
        User savedJoinSoloUser = userRepository.save(joinSoloUser);
    
        // In solo mode, the team parameter is not required. Pass null.
        LobbyJoinResponseDTO soloJoinResponse = lobbyService.joinLobby(
            soloLobby.getId(),
            savedJoinSoloUser.getId(),
            null,
            soloLobby.getLobbyCode(),
            false
        );
    
        assertNotNull(soloJoinResponse);
        assertEquals("Joined lobby successfully.", soloJoinResponse.getMessage());
    }
}