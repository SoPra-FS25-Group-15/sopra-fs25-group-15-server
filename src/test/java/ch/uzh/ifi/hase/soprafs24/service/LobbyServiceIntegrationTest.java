package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
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
        
        // Create a team lobby (public - ranked)
        teamLobby = new Lobby();
        teamLobby.setPrivate(false); // Ranked games are public
        teamLobby.setMode(LobbyConstants.MODE_TEAM);
        teamLobby.setHost(hostUser);
        teamLobby = lobbyService.createLobby(teamLobby);
        
        // Create a casual lobby (private - unranked) - should enforce solo mode
        soloLobby = new Lobby();
        soloLobby.setPrivate(true); // Unranked games are private
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
        // Verify lobby code is generated for ranked lobby too
        assertNotNull(teamLobby.getLobbyCode());
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
    public void testCreateLobby_ProvidesDefaultRoundCards() {
        // Create a new lobby
        Lobby newLobby = new Lobby();
        newLobby.setPrivate(true);  // casual/unranked game
        newLobby.setMode(LobbyConstants.MODE_SOLO);  // solo mode
        newLobby.setHost(hostUser);
        
        // Create the lobby through the service
        Lobby createdLobby = lobbyService.createLobby(newLobby);
        
        // Verify the lobby has server-generated round cards
        assertNotNull(createdLobby.getHintsEnabled());
        assertFalse(createdLobby.getHintsEnabled().isEmpty());
        assertEquals(5, createdLobby.getHintsEnabled().size());  // Checking for the 5 default cards
        
        // Verify the DTO mapper includes these cards in the response
        LobbyResponseDTO responseDTO = mapper.lobbyEntityToResponseDTO(createdLobby);
        assertNotNull(responseDTO.getRoundCards());
        assertEquals(createdLobby.getHintsEnabled(), responseDTO.getRoundCards());
        
        // Verify the expected default card values
        assertTrue(responseDTO.getRoundCards().contains("STANDARD_CARD_1"));
        assertTrue(responseDTO.getRoundCards().contains("STANDARD_CARD_5"));
    }

    @Test
    public void testCreateLobby_GeneratesLobbyCodeForAllTypes() {
        // Create a ranked lobby (public)
        Lobby rankedLobby = new Lobby();
        rankedLobby.setPrivate(false);  // ranked/public game
        rankedLobby.setMode(LobbyConstants.MODE_TEAM);
        rankedLobby.setHost(hostUser);
        
        // Create the ranked lobby through the service
        Lobby createdRankedLobby = lobbyService.createLobby(rankedLobby);
        
        // Verify lobby code is generated for ranked lobby
        assertNotNull(createdRankedLobby.getLobbyCode());
        
        // Create an unranked lobby (private)
        Lobby unrankedLobby = new Lobby();
        unrankedLobby.setPrivate(true);  // unranked/private game
        unrankedLobby.setHost(hostUser);
        
        // Create the unranked lobby through the service
        Lobby createdUnrankedLobby = lobbyService.createLobby(unrankedLobby);
        
        // Verify lobby code is also generated for unranked lobby
        assertNotNull(createdUnrankedLobby.getLobbyCode());
    }

    @Test
    public void testJoinLobby_WithCode_Success() {
        // Create a new user who will join
        User joiningUser = new User();
        joiningUser.setEmail("joiner@example.com");
        joiningUser.setPassword("password");
        joiningUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the joining user
        UserProfile joinerProfile = new UserProfile();
        joinerProfile.setUsername("JoiningUser");
        joinerProfile.setStatsPublic(true);
        joinerProfile.setMmr(0);
        joinerProfile.setAchievements(new ArrayList<>());
        joiningUser.setProfile(joinerProfile);
        
        // Generate token
        joiningUser.setToken("joiner-test-token");
        
        // Save the joining user
        joiningUser = userRepository.saveAndFlush(joiningUser);
        
        // Get the lobby code from the team lobby
        String lobbyCode = teamLobby.getLobbyCode();
        assertNotNull(lobbyCode);
        
        // User tries to join using lobby code (not as friend)
        LobbyJoinResponseDTO response = lobbyService.joinLobby(
            teamLobby.getId(), 
            joiningUser.getId(), 
            null, 
            lobbyCode,
            false
        );
        
        // Verify the join was successful
        assertNotNull(response);
        assertNotNull(response.getLobby());
        assertEquals(teamLobby.getId(), response.getLobby().getLobbyId());
    }
    
    @Test
    public void testJoinLobby_AssumedFriendButWrongCode_Success() {
        // Create a new user who will join
        User joiningUser = new User();
        joiningUser.setEmail("friend@example.com");
        joiningUser.setPassword("password");
        joiningUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the joining user
        UserProfile joinerProfile = new UserProfile();
        joinerProfile.setUsername("FriendUser");
        joinerProfile.setStatsPublic(true);
        joinerProfile.setMmr(0);
        joinerProfile.setAchievements(new ArrayList<>());
        joiningUser.setProfile(joinerProfile);
        
        // Generate token
        joiningUser.setToken("friend-test-token");
        
        // Save the joining user
        joiningUser = userRepository.saveAndFlush(joiningUser);
        
        // User tries to join as a friend (no code needed)
        LobbyJoinResponseDTO response = lobbyService.joinLobby(
            teamLobby.getId(), 
            joiningUser.getId(), 
            null, 
            "WRONG_CODE", // Wrong code shouldn't matter since joining as friend
            true  // Joining as friend
        );
        
        // Verify the join was successful even with wrong code
        assertNotNull(response);
        assertNotNull(response.getLobby());
        assertEquals(teamLobby.getId(), response.getLobby().getLobbyId());
    }
    
    @Test
    public void testJoinLobby_WithWrongCode_Fails() {
        // Create a new user who will join
        User joiningUser = new User();
        joiningUser.setEmail("joiner2@example.com");
        joiningUser.setPassword("password");
        joiningUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the joining user
        UserProfile joinerProfile = new UserProfile();
        joinerProfile.setUsername("JoiningUser2");
        joinerProfile.setStatsPublic(true);
        joinerProfile.setMmr(0);
        joinerProfile.setAchievements(new ArrayList<>());
        joiningUser.setProfile(joinerProfile);
        
        // Generate token
        joiningUser.setToken("joiner2-test-token");
        
        // Save the joining user
        joiningUser = userRepository.saveAndFlush(joiningUser);
        
        // User tries to join using an incorrect lobby code (not as friend)
        try {
            lobbyService.joinLobby(
                teamLobby.getId(), 
                joiningUser.getId(), 
                null, 
                "WRONG_CODE", // Wrong code
                false // Not joining as friend
            );
            
            // If we get here, no exception was thrown - fail the test
            fail("Expected exception for wrong lobby code was not thrown");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Verify the exception was for the correct reason
            assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, e.getStatus());
            assertTrue(e.getReason().contains("Invalid lobby code"));
        }
    }
    
    @Test
    public void testJoinLobby_NoCode_Fails() {
        // Create a new user who will join
        User joiningUser = new User();
        joiningUser.setEmail("joiner3@example.com");
        joiningUser.setPassword("password");
        joiningUser.setStatus(UserStatus.OFFLINE);
        
        // Create and set a profile for the joining user
        UserProfile joinerProfile = new UserProfile();
        joinerProfile.setUsername("JoiningUser3");
        joinerProfile.setStatsPublic(true);
        joinerProfile.setMmr(0);
        joinerProfile.setAchievements(new ArrayList<>());
        joiningUser.setProfile(joinerProfile);
        
        // Generate token
        joiningUser.setToken("joiner3-test-token");
        
        // Save the joining user
        joiningUser = userRepository.saveAndFlush(joiningUser);
        
        // User tries to join without providing a lobby code (not as friend)
        try {
            lobbyService.joinLobby(
                teamLobby.getId(), 
                joiningUser.getId(), 
                null, 
                null, // No code
                false // Not joining as friend
            );
            
            // If we get here, no exception was thrown - fail the test
            fail("Expected exception for missing lobby code was not thrown");
        } catch (org.springframework.web.server.ResponseStatusException e) {
            // Verify the exception was for the correct reason
            assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, e.getStatus());
            assertTrue(e.getReason().contains("Invalid lobby code"));
        }
    }
}