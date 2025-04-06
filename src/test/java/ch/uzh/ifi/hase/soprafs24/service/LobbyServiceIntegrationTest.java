package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

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
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;

@WebAppConfiguration
@SpringBootTest
@Transactional
public class LobbyServiceIntegrationTest {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthService authService;

    @Autowired
    private DTOMapper mapper;

    private User hostUser;
    private Lobby teamLobby;
    private Lobby soloLobby;

    @BeforeEach
    public void setup() {
        // Clear repositories before each test.
        lobbyRepository.deleteAll();
        userRepository.deleteAll();
    
        // Create and persist a host user
        hostUser = new User();
        hostUser.setEmail("host@example.com");
        hostUser.setPassword("password");
        hostUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the host user.
        UserProfile hostProfile = new UserProfile();
        hostProfile.setUsername("HostUser");
        hostProfile.setStatsPublic(true);
        hostProfile.setMmr(0);
        hostProfile.setAchievements(new ArrayList<>());
        hostUser.setProfile(hostProfile);
        
        // Generate authentication token - this is normally done in the auth service
        hostUser.setToken("host-test-token");
        
        // Save the host user first
        hostUser = userRepository.saveAndFlush(hostUser);
        
        // Create a team lobby (for ranked games)
        teamLobby = new Lobby();
        teamLobby.setLobbyName("Team Lobby");
        teamLobby.setGameType(LobbyConstants.GAME_TYPE_RANKED);
        teamLobby.setMode(LobbyConstants.MODE_TEAM);
        teamLobby.setHost(hostUser);
        teamLobby = lobbyService.createLobby(teamLobby);
        
        // Create a casual lobby - should enforce solo mode
        soloLobby = new Lobby();
        soloLobby.setLobbyName("Casual Lobby");
        soloLobby.setGameType(LobbyConstants.GAME_TYPE_UNRANKED);
        // Attempt to set team mode - should be overridden to solo
        soloLobby.setMode(LobbyConstants.MODE_TEAM);
        soloLobby.setHost(hostUser);
        soloLobby = lobbyService.createLobby(soloLobby);
        
        // Ensure everything is saved to the database
        lobbyRepository.flush();
    }

    @Test
    public void testCreateRankedLobby_RespectsTeamMode() {
        // The team lobby created in setup should respect team mode
        assertNotNull(teamLobby.getId());
        assertEquals(false, teamLobby.isPrivate()); // Ranked games are public
        assertEquals(LobbyConstants.MODE_TEAM, teamLobby.getMode());
        assertEquals(2, teamLobby.getMaxPlayersPerTeam()); // Team size is preserved
        assertEquals(LobbyConstants.LOBBY_STATUS_WAITING, teamLobby.getStatus());
        assertNotNull(teamLobby.getCreatedAt());
    }
    
    @Test
    public void testCreateCasualLobby_EnforcesSoloMode() {
        // The casual lobby created in setup should enforce solo mode
        assertNotNull(soloLobby.getId());
        assertTrue(soloLobby.isPrivate()); // Casual games are private
        assertEquals(LobbyConstants.MODE_SOLO, soloLobby.getMode()); // Mode is enforced to solo
        assertEquals(1, soloLobby.getMaxPlayersPerTeam()); // maxPlayersPerTeam set to 1 for solo
        assertNotNull(soloLobby.getLobbyCode()); // Should have a generated code
        
        // Check that the DTO correctly maps the solo mode lobby
        LobbyResponseDTO soloDTO = mapper.lobbyEntityToResponseDTO(soloLobby);
        assertEquals("solo", soloDTO.getMode());
        assertEquals(8, soloDTO.getMaxPlayers()); // Default maxPlayers is 8
        assertNull(soloDTO.getMaxPlayersPerTeam()); // Should not expose maxPlayersPerTeam
    }

    @Test
    public void testUpdateLobbyConfig_success() {
        LobbyConfigUpdateRequestDTO configUpdate = new LobbyConfigUpdateRequestDTO();
        configUpdate.setMode(LobbyConstants.MODE_TEAM);
        configUpdate.setMaxPlayers(6);
        List<String> roundCards = new ArrayList<>();
        roundCards.add("card1");
        roundCards.add("card2");
        configUpdate.setRoundCards(roundCards);
    
        Lobby updatedLobby = lobbyService.updateLobbyConfig(teamLobby.getId(), configUpdate, hostUser.getId());
        assertEquals(LobbyConstants.MODE_TEAM, updatedLobby.getMode());
        assertEquals(6, updatedLobby.getMaxPlayers());
        assertEquals(List.of("card1", "card2"), updatedLobby.getHintsEnabled());
    }

    @Test
    public void testInviteToLobby_WithCode() {
        // Testing code-based invitation (non-friend)
        InviteLobbyRequestDTO inviteRequest = new InviteLobbyRequestDTO();
        inviteRequest.setLobbyCode(null); // Indicates we want the lobby code
    
        LobbyInviteResponseDTO inviteDTO = lobbyService.inviteToLobby(soloLobby.getId(), hostUser.getId(), inviteRequest);
        assertNotNull(inviteDTO.getLobbyCode()); // Should return the generated code
        assertNull(inviteDTO.getInvitedFriend());
    }
    
    @Test
    public void testInviteFriendToLobby() {
        // Create a friend user with all required fields
        User friendUser = new User();
        friendUser.setEmail("friend@example.com");
        friendUser.setPassword("password");
        friendUser.setStatus(UserStatus.OFFLINE);
        UserProfile friendProfile = new UserProfile();
        friendProfile.setUsername("FriendUser");
        friendProfile.setMmr(0);
        friendProfile.setStatsPublic(true);
        friendProfile.setAchievements(new ArrayList<>());
        friendUser.setProfile(friendProfile);
        friendUser = userRepository.saveAndFlush(friendUser);
        
        // Create friend invitation
        InviteLobbyRequestDTO inviteRequest = new InviteLobbyRequestDTO();
        inviteRequest.setFriendId(friendUser.getId()); // Invite by friend ID
    
        LobbyInviteResponseDTO inviteDTO = lobbyService.inviteToLobby(soloLobby.getId(), hostUser.getId(), inviteRequest);
        assertEquals("FriendUser", inviteDTO.getInvitedFriend());
        assertNull(inviteDTO.getLobbyCode()); // No code for direct friend invites
    }
    
    @Test
    public void testJoinLobby_WithCodeInSoloMode() {
        // Create a player user with all required fields
        User playerUser = createAndSaveUser("player@example.com", "PlayerUser");
        
        // Join with code for solo lobby
        LobbyJoinResponseDTO joinResponse = lobbyService.joinLobby(
            soloLobby.getId(),
            playerUser.getId(),
            null, // No team needed for solo mode
            soloLobby.getLobbyCode(),
            false
        );
    
        assertNotNull(joinResponse);
        assertEquals("Joined lobby successfully.", joinResponse.getMessage());
        assertEquals("solo", joinResponse.getLobby().getMode());
        
        // Verify player was added to the players list - using direct DB query within transaction
        Lobby updatedLobby = lobbyRepository.findById(soloLobby.getId()).orElse(null);
        assertNotNull(updatedLobby);
        assertNotNull(updatedLobby.getPlayers());
        
        boolean playerFound = false;
        for (User player : updatedLobby.getPlayers()) {
            if (player.getId().equals(playerUser.getId())) {
                playerFound = true;
                break;
            }
        }
        assertTrue(playerFound, "Player should be in the players list");
    }
    
    @Test
    public void testJoinLobby_AsFriendInSoloMode() {
        // Create a friend user with all required fields
        User friendUser = createAndSaveUser("friend@example.com", "FriendUser");
        
        // Join as a friend for solo lobby (no code needed)
        LobbyJoinResponseDTO joinResponse = lobbyService.joinLobby(
            soloLobby.getId(),
            friendUser.getId(),
            null, // No team needed for solo mode
            null, // No code needed for friend invite
            true // Joining as friend
        );
    
        assertNotNull(joinResponse);
        assertEquals("Joined lobby successfully.", joinResponse.getMessage());
        
        // Verify friend was added to the players list - using direct DB query within transaction
        Lobby updatedLobby = lobbyRepository.findById(soloLobby.getId()).orElse(null);
        assertNotNull(updatedLobby);
        assertNotNull(updatedLobby.getPlayers());
        
        boolean friendFound = false;
        for (User player : updatedLobby.getPlayers()) {
            if (player.getId().equals(friendUser.getId())) {
                friendFound = true;
                break;
            }
        }
        assertTrue(friendFound, "Friend should be in the players list");
    }
    
    @Test
    public void testJoinLobby_WithTeamInTeamMode() {
        // Create a player user with all required fields
        User playerUser = createAndSaveUser("player@example.com", "TeamPlayer");
        
        // Join with team name for team lobby
        LobbyJoinResponseDTO joinResponse = lobbyService.joinLobby(
            teamLobby.getId(),
            playerUser.getId(),
            "blue", // Team name is required for team mode
            null, // No code needed for public ranked lobbies
            false
        );
    
        assertNotNull(joinResponse);
        assertEquals("Joined lobby successfully.", joinResponse.getMessage());
        assertEquals("team", joinResponse.getLobby().getMode());
        
        // Verify player was added to the correct team - using direct DB query within transaction
        Lobby updatedLobby = lobbyRepository.findById(teamLobby.getId()).orElse(null);
        assertNotNull(updatedLobby);
        assertNotNull(updatedLobby.getTeams());
        assertTrue(updatedLobby.getTeams().containsKey("blue"), "Blue team should exist");
        
        boolean playerInTeam = false;
        for (User player : updatedLobby.getTeams().get("blue")) {
            if (player.getId().equals(playerUser.getId())) {
                playerInTeam = true;
                break;
            }
        }
        assertTrue(playerInTeam, "Player should be in the blue team");
    }

    @Test
    public void testLeaveLobby_soloMode_userLeaves_success() {
        // Create a player with all required fields
        User soloPlayer = createAndSaveUser("soloplayer@example.com", "SoloPlayer");
        
        // Join the solo lobby first
        lobbyService.joinLobby(
            soloLobby.getId(),
            soloPlayer.getId(),
            null,
            soloLobby.getLobbyCode(),
            false
        );
        
        // Verify the player was added
        Lobby lobbyBeforeLeaving = lobbyRepository.findById(soloLobby.getId()).orElse(null);
        assertNotNull(lobbyBeforeLeaving);
        assertNotNull(lobbyBeforeLeaving.getPlayers());
        
        boolean playerAddedInitially = false;
        for (User player : lobbyBeforeLeaving.getPlayers()) {
            if (player.getId().equals(soloPlayer.getId())) {
                playerAddedInitially = true;
                break;
            }
        }
        assertTrue(playerAddedInitially, "Player should be initially added to the lobby");
        
        // Use the service to leave
        LobbyLeaveResponseDTO response = lobbyService.leaveLobby(
            soloLobby.getId(),
            soloPlayer.getId(), // player leaving themselves
            soloPlayer.getId()
        );
        
        // Verify with assertions
        assertNotNull(response);
        assertEquals("Left lobby successfully.", response.getMessage());
        
        // Direct database verification - should be within same transaction
        Lobby updatedLobby = lobbyRepository.findById(soloLobby.getId()).orElse(null);
        assertNotNull(updatedLobby);
        assertNotNull(updatedLobby.getPlayers());
        
        boolean userStillInLobby = false;
        for (User player : updatedLobby.getPlayers()) {
            if (player.getId().equals(soloPlayer.getId())) {
                userStillInLobby = true;
                break;
            }
        }
        assertFalse(userStillInLobby, "Player should be removed from the lobby");
    }
    
    // Helper method to create and save a user with all required fields
    private User createAndSaveUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("token-" + username); // Add token for authentication
        
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setMmr(0);
        profile.setStatsPublic(true); // Required field
        profile.setAchievements(new ArrayList<>());
        user.setProfile(profile);
        
        return userRepository.saveAndFlush(user); // Use saveAndFlush to ensure immediate persistence
    }
}