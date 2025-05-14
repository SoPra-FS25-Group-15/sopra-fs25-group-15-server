package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoundCardService roundCardService;

    @MockBean
    private ActionCardService actionCardService;

    @MockBean
    private LobbyService lobbyService;

    @MockBean
    private AuthService authService;

    private User testUser;
    private User testUser2;
    private User testUser3;
    private RoundCardDTO testRoundCard1;
    private RoundCardDTO testRoundCard2;
    private ActionCardDTO testActionCard1;
    private ActionCardDTO testActionCard2;
    private ActionCardDTO testActionCard3;
    private final String validToken = "Bearer valid-token";
    private final String cleanToken = "valid-token";
    private final String noPrefix = "valid-token-no-prefix";
    private final String invalidToken = "Bearer invalid-token";
    private final Long lobbyId = 1L;
    private final Long invalidLobbyId = 999L;

    @BeforeEach
    public void setup() throws Exception {
        // Create test users
        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);

        testUser2 = new User();
        idField.set(testUser2, 2L);

        testUser3 = new User();
        idField.set(testUser3, 3L);

        // Setup round cards
        testRoundCard1 = new RoundCardDTO();
        testRoundCard1.setId("world-1");
        testRoundCard1.setName("World");
        testRoundCard1.setDescription("Standard round on world map");

        testRoundCard2 = new RoundCardDTO();
        testRoundCard2.setId("flash-1");
        testRoundCard2.setName("Flash");
        testRoundCard2.setDescription("Quick round with limited time");

        // Setup action cards with unique values
        testActionCard1 = new ActionCardDTO();
        testActionCard1.setId("7choices");
        testActionCard1.setType("powerup");
        testActionCard1.setTitle("7 Choices");
        testActionCard1.setDescription("Reveal continent information");

        testActionCard2 = new ActionCardDTO();
        testActionCard2.setId("swap");
        testActionCard2.setType("attack");
        testActionCard2.setTitle("Swap");
        testActionCard2.setDescription("Swap positions with another player");

        testActionCard3 = new ActionCardDTO();
        testActionCard3.setId("skip");
        testActionCard3.setType("defense");
        testActionCard3.setTitle("Skip");
        testActionCard3.setDescription("Skip one guess");

        // Configure default mock responses
        when(authService.getUserByToken(cleanToken)).thenReturn(testUser);
        when(authService.getUserByToken(noPrefix)).thenReturn(testUser);
        when(authService.getUserByToken("invalid-token")).thenReturn(null);
        when(roundCardService.getAllRoundCards()).thenReturn(Arrays.asList(testRoundCard1, testRoundCard2));
    }

    @Test
    public void getGameData_ValidHostToken_ReturnsGameData() throws Exception {
        // Given - user is host with 3 players
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L, 3L));

        // Set up different action cards for each player
        when(actionCardService.drawRandomCard())
                .thenReturn(testActionCard1)
                .thenReturn(testActionCard2)
                .thenReturn(testActionCard3);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards", hasSize(2)))
                .andExpect(jsonPath("$.roundCards[0].id").value("world-1"))
                .andExpect(jsonPath("$.roundCards[0].name").value("World"))
                .andExpect(jsonPath("$.roundCards[0].description").value("Standard round on world map"))
                .andExpect(jsonPath("$.roundCards[1].id").value("flash-1"))
                .andExpect(jsonPath("$.roundCards[1].name").value("Flash"))
                .andExpect(jsonPath("$.actionCards").exists())
                .andExpect(jsonPath("$.actionCards['1'][0].id").value("7choices"))
                .andExpect(jsonPath("$.actionCards['1'][0].title").value("7 Choices"))
                .andExpect(jsonPath("$.actionCards['2'][0].id").value("swap"))
                .andExpect(jsonPath("$.actionCards['3'][0].id").value("skip"));

        // Verify methods were called the right number of times
        verify(actionCardService, times(3)).drawRandomCard();
        verify(roundCardService, times(1)).getAllRoundCards();
        verify(lobbyService, times(1)).getLobbyPlayerIds(lobbyId);
    }

    @Test
    public void getGameData_ValidPlayerToken_ReturnsGameData() throws Exception {
        // Given - user is player but not host
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenReturn(Arrays.asList(cleanToken, "other-token"));
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));

        // Set up action cards
        when(actionCardService.drawRandomCard())
                .thenReturn(testActionCard1)
                .thenReturn(testActionCard2);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards", hasSize(2)))
                .andExpect(jsonPath("$.actionCards").exists())
                .andExpect(jsonPath("$.actionCards['1'][0].id").value("7choices"))
                .andExpect(jsonPath("$.actionCards['2'][0].id").value("swap"));

        // Verify correct tokens were checked
        verify(lobbyService, times(1)).getLobbyPlayerTokens(lobbyId);
    }

    @Test
    public void getGameData_TokenWithoutBearer_StillWorks() throws Exception {
        // Given - token without Bearer prefix
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(noPrefix))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", noPrefix)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards").exists())
                .andExpect(jsonPath("$.actionCards").exists());

        // Verify token extraction worked correctly
        verify(authService, times(1)).getUserByToken(noPrefix);
    }

    @Test
    public void getGameData_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Given - invalid token
        when(authService.getUserByToken("invalid-token")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameData_UserNotInLobby_ReturnsForbidden() throws Exception {
        // Given - user not in lobby
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenReturn(Collections.singletonList("different-token"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getGameData_NoLobbyId_ReturnsBadRequest() throws Exception {
        // When & Then - missing required parameter
        mockMvc.perform(get("/games/data")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getGameData_InvalidLobbyId_ReturnsBadRequest() throws Exception {
        // Given - invalid lobbyId format
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", "not-a-number")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getGameData_LobbyNotFound_ReturnsNotFound() throws Exception {
        // Given - lobby not found
        when(authService.getUserByToken(cleanToken)).thenReturn(testUser);
        when(lobbyService.isUserHostByToken(eq(invalidLobbyId), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", invalidLobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getGameData_RoundCardServiceException_ReturnsInternalServerError() throws Exception {
        // Given - round card service throws exception
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(roundCardService.getAllRoundCards()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_ActionCardServiceException_ReturnsInternalServerError() throws Exception {
        // Given - action card service throws exception
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L));
        when(actionCardService.drawRandomCard()).thenThrow(new RuntimeException("Action card error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_LobbyPlayerIdsException_ReturnsInternalServerError() throws Exception {
        // Given - lobby service throws exception when getting player IDs
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(anyLong())).thenThrow(new RuntimeException("Player IDs error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_NoAuthHeader_ReturnsBadRequest() throws Exception {
        // When & Then - missing auth header
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getGameData_EmptyLobby_ReturnsSuccess() throws Exception {
        // Given - lobby exists but has no players
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards", hasSize(2)))
                .andExpect(jsonPath("$.actionCards").exists());
    }

    @Test
    public void getGameData_PlayerTokensError_ReturnsNotFound() throws Exception {
        // membership check throws a 404
        when(authService.getUserByToken(cleanToken)).thenReturn(testUser);
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(eq(lobbyId)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNotFound());
    }

    @Test
    public void getGameData_RoundCardServiceStatusException_ReturnsServiceUnavailable() throws Exception {
        // RoundCardService throws a ResponseStatusException → propagate 503
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(roundCardService.getAllRoundCards())
            .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Maintenance"));

        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isServiceUnavailable());
    }

    @Test
    public void getGameData_ActionCardServiceStatusException_ReturnsServiceUnavailable() throws Exception {
        // drawRandomCard throws a ResponseStatusException → propagate 503
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L));
        when(actionCardService.drawRandomCard())
            .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Rate limit"));

        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isServiceUnavailable());
    }


}