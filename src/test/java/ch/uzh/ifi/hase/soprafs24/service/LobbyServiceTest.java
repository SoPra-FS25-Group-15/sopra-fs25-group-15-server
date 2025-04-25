package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyInviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LobbyServiceTest {

    @Mock
    private LobbyRepository lobbyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DTOMapper mapper;

    @InjectMocks
    private LobbyService lobbyService;

    private User testUser;
    private User testUser2;
    private Lobby testLobby;
    private final Long LOBBY_ID = 1L;
    private final String LOBBY_CODE = "12345";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test users
        testUser = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        testUser.setToken("user1-token");
        UserProfile profile1 = new UserProfile();
        profile1.setUsername("user1");
        testUser.setProfile(profile1);

        testUser2 = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser2, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        testUser2.setToken("user2-token");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("user2");
        testUser2.setProfile(profile2);

        // Create test lobby
        testLobby = new Lobby();
        // Setting ID using reflection since setId method is not available
        try {
            Field idField = Lobby.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testLobby, LOBBY_ID);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set lobby ID via reflection", e);
        }
        testLobby.setHost(testUser);
        testLobby.setLobbyCode(LOBBY_CODE);
        testLobby.setMode(LobbyConstants.MODE_SOLO);
        testLobby.setMaxPlayers(8);
        testLobby.setPlayers(new ArrayList<>(Collections.singletonList(testUser)));
        testLobby.setStatus(LobbyConstants.LOBBY_STATUS_WAITING); // Fixed: changed from LOBBY_STATUS_OPEN

        // Setup repository mock behaviors
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(testLobby));
        when(lobbyRepository.findByLobbyCode(LOBBY_CODE)).thenReturn(testLobby);
        when(lobbyRepository.save(any(Lobby.class))).thenReturn(testLobby);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
    }

    @Test
    void createLobby_soloMode() {
        // Create a lobby with the solo mode
        Lobby lobby = new Lobby();
        lobby.setMode(LobbyConstants.MODE_SOLO);
        lobby.setHost(testUser);
        
        // The service modifies the lobby and returns the saved version
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby savedLobby = invocation.getArgument(0);
            // Return the saved lobby with modifications made by the service
            return savedLobby;
        });
        
        Lobby result = lobbyService.createLobby(lobby);
        
        assertNotNull(result);
        assertEquals(LobbyConstants.MODE_SOLO, result.getMode());
        assertNotNull(result.getLobbyCode());
        assertEquals(1, result.getMaxPlayersPerTeam());
        assertEquals(8, result.getMaxPlayers());
        assertNotNull(result.getPlayers());
        assertTrue(result.getPlayers().contains(testUser));
        
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void createLobby_teamMode() {
        // Create a lobby with team mode - CRITICAL: must set isPrivate=false
        // If lobby is private, service will force mode to solo
        Lobby lobby = new Lobby();
        lobby.setMode(LobbyConstants.MODE_TEAM);
        lobby.setHost(testUser);
        lobby.setPrivate(false); // Required to maintain team mode
        
        // The service modifies the lobby and returns the saved version
        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(invocation -> {
            Lobby savedLobby = invocation.getArgument(0);
            // Return the saved lobby with modifications made by the service
            return savedLobby;
        });
        
        Lobby result = lobbyService.createLobby(lobby);
        
        assertNotNull(result);
        assertEquals(LobbyConstants.MODE_TEAM, result.getMode());
        assertEquals(2, result.getMaxPlayersPerTeam());
        assertEquals(8, result.getMaxPlayers());
        assertNotNull(result.getTeams());
        assertTrue(result.getTeams().containsKey("team1"));
        assertTrue(result.getTeams().get("team1").contains(testUser));
        
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void updateLobbyConfig_success() {
        LobbyConfigUpdateRequestDTO config = new LobbyConfigUpdateRequestDTO();
        config.setMode(LobbyConstants.MODE_TEAM);
        config.setMaxPlayers(6);
        config.setMaxPlayersPerTeam(3);
        
        Lobby result = lobbyService.updateLobbyConfig(LOBBY_ID, config, testUser.getId());
        
        assertEquals(LobbyConstants.MODE_TEAM, result.getMode());
        assertEquals(6, result.getMaxPlayers());
        assertEquals(3, result.getMaxPlayersPerTeam());
        assertNotNull(result.getTeams());
        assertNull(result.getPlayers());
        
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void updateLobbyConfig_notHost() {
        LobbyConfigUpdateRequestDTO config = new LobbyConfigUpdateRequestDTO();
        config.setMode(LobbyConstants.MODE_TEAM);
        
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.updateLobbyConfig(LOBBY_ID, config, 999L);
        });
    }

    @Test
    void joinLobby_success() {
        // Prepare mock for mapper
        when(mapper.lobbyEntityToResponseDTO(any(Lobby.class))).thenReturn(null);
        
        LobbyJoinResponseDTO response = lobbyService.joinLobby(LOBBY_ID, testUser2.getId(), null, LOBBY_CODE, false);
        
        assertNotNull(response);
        assertEquals("Joined lobby successfully.", response.getMessage());
        assertTrue(testLobby.getPlayers().contains(testUser2));
        
        verify(lobbyRepository).save(testLobby);
    }

    @Test
    void joinLobby_invalidCode() {
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.joinLobby(LOBBY_ID, testUser2.getId(), null, "wrong-code", false);
        });
    }

    @Test
    void getLobbyById_success() {
        Lobby result = lobbyService.getLobbyById(LOBBY_ID);
        assertEquals(testLobby, result);
    }

    @Test
    void getLobbyById_notFound() {
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.getLobbyById(999L);
        });
    }

    @Test
    void getLobbyByCode_success() {
        Lobby result = lobbyService.getLobbyByCode(LOBBY_CODE);
        assertEquals(testLobby, result);
    }

    @Test
    void getLobbyByCode_notFound() {
        when(lobbyRepository.findByLobbyCode("wrong-code")).thenReturn(null);
        
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.getLobbyByCode("wrong-code");
        });
    }

    @Test
    void inviteToLobby_success() {
        InviteLobbyRequestDTO request = new InviteLobbyRequestDTO();
        request.setFriendId(testUser2.getId());
        
        // Mock the response
        LobbyInviteResponseDTO mockedResponse = new LobbyInviteResponseDTO(null, testUser2.getProfile().getUsername());
        when(userRepository.findById(testUser2.getId())).thenReturn(Optional.of(testUser2));
        
        LobbyInviteResponseDTO response = lobbyService.inviteToLobby(LOBBY_ID, testUser.getId(), request);
        
        assertNotNull(response);
        // Fix: Use the correct getter method name for this DTO
        assertEquals(testUser2.getProfile().getUsername(), response.getInvitedFriend());
    }

    @Test
    void inviteToLobby_notHost() {
        InviteLobbyRequestDTO request = new InviteLobbyRequestDTO();
        request.setFriendId(testUser2.getId());
        
        assertThrows(ResponseStatusException.class, () -> {
            lobbyService.inviteToLobby(LOBBY_ID, 999L, request);
        });
    }

    @Test
    void getLobbyPlayerIds_soloMode() {
        List<Long> playerIds = lobbyService.getLobbyPlayerIds(LOBBY_ID);
        
        assertNotNull(playerIds);
        assertEquals(1, playerIds.size());
        assertTrue(playerIds.contains(testUser.getId()));
    }

    @Test
    void isUserHost_true() {
        boolean result = lobbyService.isUserHost(LOBBY_ID, testUser.getId());
        assertTrue(result);
    }

    @Test
    void isUserHost_false() {
        boolean result = lobbyService.isUserHost(LOBBY_ID, 999L);
        assertFalse(result);
    }

    @Test
    void isUserInLobby_true() {
        boolean result = lobbyService.isUserInLobby(testUser.getId(), LOBBY_ID);
        assertTrue(result);
    }
}
