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

    // ===============================
    // COMPREHENSIVE ERROR-HANDLING TESTS
    // ===============================

    @Test
    public void getGameData_EmptyAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        // Given - empty authorization header results in empty token
        when(authService.getUserByToken("")).thenReturn(null);
        
        // When & Then - empty authorization header
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameData_BearerWithoutToken_ReturnsUnauthorized() throws Exception {
        // Given - authorization header with "Bearer " but no token
        when(authService.getUserByToken("")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameData_BearerWithWhitespaceToken_ReturnsUnauthorized() throws Exception {
        // Given - authorization header with whitespace token
        when(authService.getUserByToken("   ")).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", "Bearer    ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameData_AuthServiceThrowsException_ReturnsInternalServerError() throws Exception {
        // Given - auth service throws runtime exception
        when(authService.getUserByToken(cleanToken)).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_GetLobbyPlayerTokensException_ReturnsInternalServerError() throws Exception {
        // Given - lobby service throws exception when getting player tokens
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenThrow(new RuntimeException("Player tokens error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_IsUserHostByTokenException_ReturnsInternalServerError() throws Exception {
        // Given - lobby service throws exception when checking host status
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken)))
                .thenThrow(new RuntimeException("Host check error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_NegativeLobbyId_ProcessesNormally() throws Exception {
        // Given - negative lobby ID (should be processed normally, let service layer handle validation)
        Long negativeLobbyId = -1L;
        when(lobbyService.isUserHostByToken(eq(negativeLobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(negativeLobbyId)).thenReturn(Collections.singletonList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", negativeLobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getGameData_ZeroLobbyId_ProcessesNormally() throws Exception {
        // Given - zero lobby ID (should be processed normally, let service layer handle validation)
        Long zeroLobbyId = 0L;
        when(lobbyService.isUserHostByToken(eq(zeroLobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(zeroLobbyId)).thenReturn(Collections.singletonList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", zeroLobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getGameData_RoundCardServiceReturnsNull_ReturnsSuccessWithNullRoundCards() throws Exception {
        // Given - round card service returns null (controller doesn't validate this)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Collections.emptyList());
        when(roundCardService.getAllRoundCards()).thenReturn(null);

        // When & Then - controller allows null round cards
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards").doesNotExist());
    }

    @Test
    public void getGameData_RoundCardServiceReturnsEmptyList_ReturnsSuccess() throws Exception {
        // Given - round card service returns empty list (valid scenario)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Collections.singletonList(1L));
        when(roundCardService.getAllRoundCards()).thenReturn(Collections.emptyList());
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards", hasSize(0)))
                .andExpect(jsonPath("$.actionCards").exists());
    }

    @Test
    public void getGameData_ActionCardServiceReturnsNull_ReturnsSuccessWithNullActionCard() throws Exception {
        // Given - action card service returns null card (controller doesn't validate this)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Collections.singletonList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(null);

        // When & Then - controller allows null action cards
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionCards['1'][0]").doesNotExist());
    }

    @Test
    public void getGameData_PlayerTokensListIsNull_ReturnsInternalServerError() throws Exception {
        // Given - user is not host and player tokens list is null (causes NullPointerException in contains check)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenReturn(null);

        // When & Then - NullPointerException converted to 500
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_PlayerIdsListIsNull_ReturnsInternalServerError() throws Exception {
        // Given - player IDs list is null
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_PlayerIdsContainsNullValues_ReturnsInternalServerError() throws Exception {
        // Given - player IDs list contains null values (will cause NullPointerException in for-each loop)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, null, 3L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then - controller fails when trying to use null as map key
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_VeryLargeLobbyId_ProcessesNormally() throws Exception {
        // Given - very large lobby ID (edge case for Long values)
        Long largeLobbyId = Long.MAX_VALUE;
        when(lobbyService.isUserHostByToken(eq(largeLobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(largeLobbyId)).thenReturn(Collections.singletonList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", largeLobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getGameData_SpecialCharactersInToken_HandledGracefully() throws Exception {
        // Given - token with special characters
        String specialToken = "token-with-special-chars-!@#$%^&*()";
        when(authService.getUserByToken(specialToken)).thenReturn(testUser);
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(specialToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Collections.singletonList(1L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", "Bearer " + specialToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getGameData_UserBothHostAndPlayer_ReturnsSuccess() throws Exception {
        // Given - user is both host and in player list (edge case but should work)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenReturn(Arrays.asList(cleanToken, "other-token"));
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard1).thenReturn(testActionCard2);

        // When & Then
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionCards['1'][0].id").value("7choices"))
                .andExpect(jsonPath("$.actionCards['2'][0].id").value("swap"));
    }

    @Test
    public void getGameData_MultipleConsecutiveServiceExceptions_ReturnsInternalServerError() throws Exception {
        // Given - multiple service calls that could throw exceptions
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(roundCardService.getAllRoundCards()).thenThrow(new RuntimeException("Round card error"));
        
        // When & Then - should catch the first exception and return 500
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameData_ResponseStatusExceptionRethrown_MaintainsOriginalStatus() throws Exception {
        // Given - service throws ResponseStatusException (should be re-thrown as is)
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Lobby in invalid state"));

        // When & Then - should maintain original status code
        mockMvc.perform(get("/games/data")
                        .param("lobbyId", lobbyId.toString())
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }
}