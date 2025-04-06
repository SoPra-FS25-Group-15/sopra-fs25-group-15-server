package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    // ...existing code...
}