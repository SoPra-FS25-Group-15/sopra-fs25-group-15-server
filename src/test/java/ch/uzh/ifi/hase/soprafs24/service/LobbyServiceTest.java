package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true",
        "jwt.secret=test-secret",
        "google.maps.api.key=TEST_KEY"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
public class LobbyServiceTest {

    private static final Long LOBBY_ID = 1L;
    private static final String LOBBY_CODE = "12345";

    @Autowired
    private LobbyService lobbyService;

    @MockBean
    private LobbyRepository lobbyRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DTOMapper mapper;

    private User testUser;
    private User testUser2;
    private Lobby testLobby;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize mocks
        // Create test users
        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);
        testUser.setToken("user1-token");
        UserProfile profile1 = new UserProfile();
        profile1.setUsername("user1");
        testUser.setProfile(profile1);

        testUser2 = new User();
        Field idField2 = User.class.getDeclaredField("id");
        idField2.setAccessible(true);
        idField2.set(testUser2, 2L);
        testUser2.setToken("user2-token");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("user2");
        testUser2.setProfile(profile2);

        // Create test lobby
        testLobby = new Lobby();
        Field lobbyIdField = Lobby.class.getDeclaredField("id");
        lobbyIdField.setAccessible(true);
        lobbyIdField.set(testLobby, LOBBY_ID);
        testLobby.setHost(testUser);
        testLobby.setLobbyCode(LOBBY_CODE);
        testLobby.setMode(LobbyConstants.MODE_SOLO);
        testLobby.setMaxPlayers(8);
        testLobby.setPlayers(new ArrayList<>(Collections.singletonList(testUser)));
        testLobby.setStatus(LobbyConstants.LOBBY_STATUS_WAITING);

        // Stub repository methods
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
        Lobby newLobby = new Lobby();
        newLobby.setMode(LobbyConstants.MODE_TEAM);
        newLobby.setHost(testUser);
        newLobby.setPrivate(false);

        when(lobbyRepository.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

        Lobby result = lobbyService.createLobby(newLobby);

        assertNotNull(result);
        assertEquals(LobbyConstants.MODE_TEAM, result.getMode());
        assertEquals(2, result.getMaxPlayersPerTeam());
        assertEquals(8, result.getMaxPlayers());
        assertNotNull(result.getTeams());
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
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.updateLobbyConfig(LOBBY_ID, config, 999L));
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
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(
                        LOBBY_ID, testUser2.getId(), null, "wrong-code", false));
    }

    @Test
    void getLobbyById_success() {
        Lobby result = lobbyService.getLobbyById(LOBBY_ID);
        assertEquals(testLobby, result);
    }

    @Test
    void getLobbyById_notFound() {
        when(lobbyRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyById(999L));
    }

    @Test
    void getLobbyByCode_success() {
        Lobby result = lobbyService.getLobbyByCode(LOBBY_CODE);
        assertEquals(testLobby, result);
    }

    @Test
    void getLobbyByCode_notFound() {
        when(lobbyRepository.findByLobbyCode("wrong-code")).thenReturn(null);
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.getLobbyByCode("wrong-code"));
    }

    @Test
    void inviteToLobby_success() {
        InviteLobbyRequestDTO request = new InviteLobbyRequestDTO();
        request.setFriendId(testUser2.getId());
        when(userRepository.findById(testUser2.getId()))
                .thenReturn(Optional.of(testUser2));

        LobbyInviteResponseDTO response = lobbyService.inviteToLobby(
                LOBBY_ID, testUser.getId(), request);

        assertEquals(testUser2.getProfile().getUsername(), response.getInvitedFriend());
    }

    @Test
    void inviteToLobby_notHost() {
        InviteLobbyRequestDTO request = new InviteLobbyRequestDTO();
        request.setFriendId(testUser2.getId());
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.inviteToLobby(LOBBY_ID, 999L, request));
    }

    @Test
    void getLobbyPlayerIds_soloMode() {
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(testLobby));
        List<Long> playerIds = lobbyService.getLobbyPlayerIds(LOBBY_ID);
        assertEquals(1, playerIds.size());
        assertTrue(playerIds.contains(testUser.getId()));
    }

    @Test
    void isUserHost_true() {
        assertTrue(lobbyService.isUserHost(LOBBY_ID, testUser.getId()));
    }

    @Test
    void isUserHost_false() {
        assertFalse(lobbyService.isUserHost(LOBBY_ID, 999L));
    }

    @Test
    void isUserInLobby_true() {
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(testLobby));
        assertTrue(lobbyService.isUserInLobby(testUser.getId(), LOBBY_ID));
    }

    @Test
    void isUserInLobby_false() {
        when(lobbyRepository.findById(LOBBY_ID)).thenReturn(Optional.of(testLobby));
        assertFalse(lobbyService.isUserInLobby(testUser2.getId(), 999L));
    }

    @Test
    void updateLobbyStatus_success() {
        String newStatus = LobbyConstants.LOBBY_STATUS_IN_PROGRESS;

        Lobby result = lobbyService.updateLobbyStatus(LOBBY_ID, newStatus);

        assertEquals(newStatus, result.getStatus());
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void leaveLobby_playerLeaving_success() {

        testLobby.getPlayers().add(testUser2);


        when(mapper.toLobbyLeaveResponse(any(Lobby.class), anyString())).thenReturn(new LobbyLeaveResponseDTO("Left lobby successfully.", null));


        LobbyLeaveResponseDTO response = lobbyService.leaveLobby(LOBBY_ID, testUser2.getId(), testUser2.getId());

        assertNotNull(response);
        assertEquals("Left lobby successfully.", response.getMessage());
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void leaveLobby_hostKickingPlayer_success() {

        testLobby.getPlayers().add(testUser2);


        when(mapper.toLobbyLeaveResponse(any(Lobby.class), anyString())).thenReturn(new LobbyLeaveResponseDTO("Left lobby successfully.", null));


        LobbyLeaveResponseDTO response = lobbyService.leaveLobby(LOBBY_ID, testUser.getId(), testUser2.getId());

        assertNotNull(response);
        assertEquals("Left lobby successfully.", response.getMessage());
        verify(lobbyRepository).save(any(Lobby.class));
    }

    @Test
    void leaveLobby_nonHostKickingPlayer_throws() {

        testLobby.getPlayers().add(testUser2);


        assertThrows(ResponseStatusException.class,
                () -> lobbyService.leaveLobby(LOBBY_ID, testUser2.getId(), testUser.getId()));
    }

    @Test
    void leaveLobby_userNotInLobby_throws() {
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.leaveLobby(LOBBY_ID, testUser.getId(), 999L));
    }

    @Test
    void listLobbies_success() {
        List<Lobby> lobbyList = new ArrayList<>();
        lobbyList.add(testLobby);

        when(lobbyRepository.findAll()).thenReturn(lobbyList);

        List<Lobby> result = lobbyService.listLobbies();

        assertEquals(1, result.size());
        assertEquals(testLobby, result.get(0));
    }

    @Test
    void listLobbies_noLobbies_throws() {
        when(lobbyRepository.findAll()).thenReturn(new ArrayList<>());

        assertThrows(ResponseStatusException.class, () -> lobbyService.listLobbies());
    }

    @Test
    void deleteLobby_success() {
        doNothing().when(lobbyRepository).delete(any(Lobby.class));

        var response = lobbyService.deleteLobby(LOBBY_ID, testUser.getId());

        assertEquals("Lobby disbanded successfully.", response.getMessage());
        verify(lobbyRepository).delete(testLobby);
    }

    @Test
    void deleteLobby_notHost_throws() {
        assertThrows(ResponseStatusException.class,
                () -> lobbyService.deleteLobby(LOBBY_ID, testUser2.getId()));
    }

    @Test
    void getCurrentLobbyForUser_userInLobby_returnsLobby() {
        List<Lobby> lobbies = Collections.singletonList(testLobby);
        when(lobbyRepository.findAll()).thenReturn(lobbies);

        Lobby result = lobbyService.getCurrentLobbyForUser(testUser.getId());

        assertEquals(testLobby, result);
    }

    @Test
    void getCurrentLobbyForUser_userNotFound() {

        when(userRepository.findById(999L)).thenReturn(Optional.empty());


        assertThrows(ResponseStatusException.class, () -> lobbyService.getCurrentLobbyForUser(999L));
    }

    @Test
    void getCurrentLobbyForUser_userExistsButNotInLobby_returnsNull() {

        User outsideUser = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(outsideUser, 3L);
            outsideUser.setToken("outside-token");
        } catch (Exception e) {
            fail("Failed to set up outside user");
        }


        when(userRepository.findById(3L)).thenReturn(Optional.of(outsideUser));


        List<Lobby> lobbies = Collections.singletonList(testLobby);
        when(lobbyRepository.findAll()).thenReturn(lobbies);


        Lobby result = lobbyService.getCurrentLobbyForUser(3L);


        assertNull(result);
    }

    @Test
    void getLobbyPlayerTokens_soloMode() {
        List<String> tokens = lobbyService.getLobbyPlayerTokens(LOBBY_ID);

        assertEquals(1, tokens.size());
        assertTrue(tokens.contains(testUser.getToken()));
    }

    @Test
    void getLobbyPlayerTokens_teamMode() {

        testLobby.setMode(LobbyConstants.MODE_TEAM);
        testLobby.setPlayers(null);


        Map<String, List<User>> teams = new HashMap<>();
        teams.put("team1", new ArrayList<>(Collections.singletonList(testUser)));
        teams.put("team2", new ArrayList<>(Collections.singletonList(testUser2)));
        testLobby.setTeams(teams);

        List<String> tokens = lobbyService.getLobbyPlayerTokens(LOBBY_ID);

        assertEquals(2, tokens.size());
        assertTrue(tokens.contains(testUser.getToken()));
        assertTrue(tokens.contains(testUser2.getToken()));
    }

    @Test
    void isUserHostByToken_tokenMatches_returnsTrue() {
        boolean result = lobbyService.isUserHostByToken(LOBBY_ID, testUser.getToken());

        assertTrue(result);
    }

    @Test
    void isUserHostByToken_tokenDoesNotMatch_returnsFalse() {
        boolean result = lobbyService.isUserHostByToken(LOBBY_ID, "wrong-token");

        assertFalse(result);
    }

    @Test
    void handleUserDisconnect_userIsHost_deletesLobby() {
        List<Lobby> lobbies = Collections.singletonList(testLobby);
        when(lobbyRepository.findAll()).thenReturn(lobbies);


        doNothing().when(lobbyRepository).delete(any(Lobby.class));

        lobbyService.handleUserDisconnect(testUser.getId());


        verify(lobbyRepository).delete(any(Lobby.class));
    }

    @Test
    void handleUserDisconnect_userIsPlayer_removesFromLobby() {

        testLobby.getPlayers().add(testUser2);

        List<Lobby> lobbies = Collections.singletonList(testLobby);
        when(lobbyRepository.findAll()).thenReturn(lobbies);


        when(mapper.toLobbyLeaveResponse(any(Lobby.class), anyString())).thenReturn(new LobbyLeaveResponseDTO("Left lobby successfully.", null));

        lobbyService.handleUserDisconnect(testUser2.getId());


        verify(lobbyRepository).save(any(Lobby.class));

    }

    @Test
    void joinLobby_lobbyFull_throws() {

        testLobby.setMaxPlayers(1);

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(LOBBY_ID, testUser2.getId(), null, LOBBY_CODE, false));
    }

    @Test
    void joinLobby_withTeamMode_success() {

        testLobby.setMode(LobbyConstants.MODE_TEAM);
        testLobby.setPlayers(null);
        testLobby.setMaxPlayersPerTeam(2);

        Map<String, List<User>> teams = new HashMap<>();
        teams.put("team1", new ArrayList<>(Collections.singletonList(testUser)));
        testLobby.setTeams(teams);


        when(mapper.lobbyEntityToResponseDTO(any(Lobby.class))).thenReturn(null);

        LobbyJoinResponseDTO response = lobbyService.joinLobby(LOBBY_ID, testUser2.getId(), null, LOBBY_CODE, false);

        assertNotNull(response);
        assertEquals("Joined lobby successfully.", response.getMessage());
        assertTrue(testLobby.getTeams().get("team1").contains(testUser2));

        verify(lobbyRepository).save(testLobby);
    }

    @Test
    void joinLobby_team1Full_joinsTeam2() {

        testLobby.setMode(LobbyConstants.MODE_TEAM);
        testLobby.setPlayers(null);
        testLobby.setMaxPlayersPerTeam(1);

        Map<String, List<User>> teams = new HashMap<>();
        teams.put("team1", new ArrayList<>(Collections.singletonList(testUser)));
        testLobby.setTeams(teams);


        when(mapper.lobbyEntityToResponseDTO(any(Lobby.class))).thenReturn(null);

        LobbyJoinResponseDTO response = lobbyService.joinLobby(LOBBY_ID, testUser2.getId(), null, LOBBY_CODE, false);

        assertNotNull(response);
        assertEquals("Joined lobby successfully.", response.getMessage());
        assertTrue(testLobby.getTeams().containsKey("team2"));
        assertTrue(testLobby.getTeams().get("team2").contains(testUser2));

        verify(lobbyRepository).save(testLobby);
    }

    @Test
    void joinLobby_allTeamsFull_throws() {

        testLobby.setMode(LobbyConstants.MODE_TEAM);
        testLobby.setPlayers(null);
        testLobby.setMaxPlayersPerTeam(1);

        User testUser3 = new User();
        try {
            Field idField3 = User.class.getDeclaredField("id");
            idField3.setAccessible(true);
            idField3.set(testUser3, 3L);
        }
        catch (Exception e) {
            fail("Failed to set up test user 3");
        }

        Map<String, List<User>> teams = new HashMap<>();
        teams.put("team1", new ArrayList<>(Collections.singletonList(testUser)));
        teams.put("team2", new ArrayList<>(Collections.singletonList(testUser2)));
        testLobby.setTeams(teams);

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinLobby(LOBBY_ID, 3L, null, LOBBY_CODE, false));
    }

    @Test
    void createLobbyInvite_success() {
        String testLobbyCode = "12345";

        LobbyService.LobbyInvite invite = lobbyService.createLobbyInvite(testUser, testUser2, testLobbyCode);

        assertNotNull(invite);
        assertEquals(testUser, invite.getSender());
        assertEquals(testLobbyCode, invite.getLobbyCode());
    }

    @Test
    void createLobbyInvite_senderNotInLobby_throws() {
        User outsider = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(outsider, 999L);
        }
        catch (Exception e) {
            fail("Failed to set up outsider user");
        }

        assertThrows(ResponseStatusException.class,
                () -> lobbyService.createLobbyInvite(outsider, testUser2, LOBBY_CODE));
    }
}
