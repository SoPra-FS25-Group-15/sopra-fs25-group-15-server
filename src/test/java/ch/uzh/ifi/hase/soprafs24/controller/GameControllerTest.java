package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.GameDataResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class GameControllerTest {

    @Mock
    private RoundCardService roundCardService;

    @Mock
    private ActionCardService actionCardService;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GameController gameController;

    private final Long LOBBY_ID = 1L;
    private final String VALID_TOKEN = "valid-token";
    private final String INVALID_TOKEN = "invalid-token";
    private final Long HOST_ID = 100L;
    private final Long PLAYER1_ID = 101L;
    private final Long PLAYER2_ID = 102L;
    private User hostUser;
    private User player1User;
    private Lobby testLobby;
    private List<RoundCardDTO> testRoundCards;
    private ActionCardDTO testActionCard1;
    private ActionCardDTO testActionCard2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Setup test users
        hostUser = new User();
        hostUser.setId(HOST_ID);
        hostUser.setUsername("host");
        hostUser.setToken("host-token");

        player1User = new User();
        player1User.setId(PLAYER1_ID);
        player1User.setUsername("player1");
        player1User.setToken(VALID_TOKEN);

        // Setup test lobby
        testLobby = new Lobby();
        testLobby.setId(LOBBY_ID);
        testLobby.setHost(hostUser);

        // Add player to the lobby
        List<User> players = new ArrayList<>();
        players.add(player1User);
        testLobby.setPlayers(players);

        // Setup test round cards
        testRoundCards = new ArrayList<>();
        RoundCardDTO roundCard1 = new RoundCardDTO();
        roundCard1.setId("1");

        RoundCardDTO roundCard2 = new RoundCardDTO();
        roundCard2.setId("2");

        testRoundCards.add(roundCard1);
        testRoundCards.add(roundCard2);

        // Setup test action cards
        testActionCard1 = new ActionCardDTO();
        testActionCard1.setId("1");
        testActionCard1.setType("DOUBLE_POINTS");
        testActionCard1.setDescription("Double your points for this round");

        testActionCard2 = new ActionCardDTO();
        testActionCard2.setId("2");
        testActionCard2.setType("HINT");
        testActionCard2.setDescription("Get a hint for this location");

        // Mock services
        when(roundCardService.getAllRoundCards()).thenReturn(testRoundCards);
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1).thenReturn(testActionCard2);
        when(authService.getUserByToken(VALID_TOKEN)).thenReturn(player1User);
        when(authService.getUserByToken("host-token")).thenReturn(hostUser);
        when(authService.getUserByToken(INVALID_TOKEN)).thenReturn(null);

        // Mock lobby service methods
        when(lobbyService.getLobbyPlayerIds(LOBBY_ID)).thenReturn(Arrays.asList(HOST_ID, PLAYER1_ID));
        when(lobbyService.getLobbyPlayerTokens(LOBBY_ID)).thenReturn(Arrays.asList("host-token", VALID_TOKEN));
        when(lobbyService.isUserHostByToken(eq(LOBBY_ID), eq("host-token"))).thenReturn(true);
        when(lobbyService.isUserHostByToken(eq(LOBBY_ID), eq(VALID_TOKEN))).thenReturn(false);
    }

    @Test
    public void getGameData_validPlayerToken_success() {
        // Call the method
        GameDataResponseDTO response = gameController.getGameData(LOBBY_ID, "Bearer " + VALID_TOKEN);

        // Verify service calls
        verify(authService).getUserByToken(VALID_TOKEN);
        verify(lobbyService).isUserHostByToken(LOBBY_ID, VALID_TOKEN);
        verify(lobbyService).getLobbyPlayerTokens(LOBBY_ID);
        verify(roundCardService).getAllRoundCards();
        verify(lobbyService).getLobbyPlayerIds(LOBBY_ID);
        verify(actionCardService, times(2)).drawRandomCard();

        // Verify response
        assertNotNull(response);
        assertEquals(testRoundCards, response.getRoundCards());
        assertEquals(2, response.getActionCards().size());
        assertTrue(response.getActionCards().containsKey(HOST_ID));
        assertTrue(response.getActionCards().containsKey(PLAYER1_ID));
        assertEquals(1, response.getActionCards().get(HOST_ID).size());
        assertEquals(1, response.getActionCards().get(PLAYER1_ID).size());
    }

    @Test
    public void getGameData_validHostToken_success() {
        // Call the method
        GameDataResponseDTO response = gameController.getGameData(LOBBY_ID, "Bearer host-token");

        // Verify service calls
        verify(authService).getUserByToken("host-token");
        verify(lobbyService).isUserHostByToken(LOBBY_ID, "host-token");
        verify(roundCardService).getAllRoundCards();
        verify(lobbyService).getLobbyPlayerIds(LOBBY_ID);
        verify(actionCardService, times(2)).drawRandomCard();

        // Verify response
        assertNotNull(response);
        assertEquals(testRoundCards, response.getRoundCards());
        assertEquals(2, response.getActionCards().size());
    }

    @Test
    public void getGameData_invalidToken_throwsException() {
        // Expect exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameController.getGameData(LOBBY_ID, "Bearer " + INVALID_TOKEN)
        );

        // Verify exception details
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid or missing token", exception.getReason());

        // Verify service calls
        verify(authService).getUserByToken(INVALID_TOKEN);
        verify(lobbyService, never()).getLobbyPlayerIds(anyLong());
        verify(roundCardService, never()).getAllRoundCards();
    }

    @Test
    public void getGameData_nonMemberToken_throwsException() {
        // Setup non-member user
        User nonMemberUser = new User();
        nonMemberUser.setId(999L);
        nonMemberUser.setToken("nonmember-token");
        when(authService.getUserByToken("nonmember-token")).thenReturn(nonMemberUser);
        when(lobbyService.isUserHostByToken(LOBBY_ID, "nonmember-token")).thenReturn(false);

        // Expect exception
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameController.getGameData(LOBBY_ID, "Bearer nonmember-token")
        );

        // Verify exception details
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("You are not a member of this lobby", exception.getReason());

        // Verify service calls
        verify(authService).getUserByToken("nonmember-token");
        verify(lobbyService).isUserHostByToken(LOBBY_ID, "nonmember-token");
        verify(lobbyService).getLobbyPlayerTokens(LOBBY_ID);
        verify(roundCardService, never()).getAllRoundCards();
    }

    @Test
    public void getGameData_randomAssignmentOfActionCards() {
        // Setup test with multiple draws returning different cards
        ActionCardDTO card1 = new ActionCardDTO();
        card1.setId("10");
        card1.setType("SKIP_ROUND");

        ActionCardDTO card2 = new ActionCardDTO();
        card2.setId("20");
        card2.setType("EXTRA_TIME");

        when(actionCardService.drawRandomCard())
                .thenReturn(card1)
                .thenReturn(card2);

        // Call the method
        GameDataResponseDTO response = gameController.getGameData(LOBBY_ID, "Bearer " + VALID_TOKEN);

        // Verify different cards were assigned
        assertEquals(card1, response.getActionCards().get(HOST_ID).get(0));
        assertEquals(card2, response.getActionCards().get(PLAYER1_ID).get(0));
        assertNotEquals(response.getActionCards().get(HOST_ID).get(0),
                response.getActionCards().get(PLAYER1_ID).get(0));
    }
}