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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private RoundCardDTO testRoundCard1;
    private RoundCardDTO testRoundCard2;
    private ActionCardDTO testActionCard;
    private final String validToken = "Bearer valid-token";
    private final String cleanToken = "valid-token";
    private final Long lobbyId = 1L;

    @BeforeEach
    public void setup() throws Exception {
        // Create test user with ID 1
        testUser = new User();
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(testUser, 1L);

        // Setup round cards
        testRoundCard1 = new RoundCardDTO();
        testRoundCard1.setId("world-1"); // Using String ID to avoid type mismatch
        testRoundCard1.setName("World");
        testRoundCard1.setDescription("Standard round on world map");

        testRoundCard2 = new RoundCardDTO();
        testRoundCard2.setId("flash-1"); // Using String ID to avoid type mismatch
        testRoundCard2.setName("Flash");
        testRoundCard2.setDescription("Quick round with limited time");

        // Setup action cards
        testActionCard = new ActionCardDTO();
        testActionCard.setId("7choices");
        testActionCard.setType("powerup");
        testActionCard.setTitle("7 Choices"); // Using setTitle instead of setName
        testActionCard.setDescription("Reveal continent information");

        // Configure default mock responses
        when(authService.getUserByToken(cleanToken)).thenReturn(testUser);
        when(roundCardService.getAllRoundCards()).thenReturn(Arrays.asList(testRoundCard1, testRoundCard2));
        when(actionCardService.drawRandomCard()).thenReturn(testActionCard);
    }

    @Test
    public void getGameData_ValidHostToken_ReturnsGameData() throws Exception {
        // Given - user is host
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));

        // When & Then
        mockMvc.perform(get("/games/data")
                .param("lobbyId", lobbyId.toString())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roundCards", hasSize(2)))
                .andExpect(jsonPath("$.roundCards[0].id").value("world-1"))
                .andExpect(jsonPath("$.roundCards[1].id").value("flash-1"))
                .andExpect(jsonPath("$.actionCards").exists())
                .andExpect(jsonPath("$.actionCards['1'][0].id").value("7choices"))
                .andExpect(jsonPath("$.actionCards['2'][0].id").value("7choices"));
    }

    @Test
    public void getGameData_ValidPlayerToken_ReturnsGameData() throws Exception {
        // Given - user is player but not host
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(false);
        when(lobbyService.getLobbyPlayerTokens(lobbyId)).thenReturn(Arrays.asList(cleanToken, "other-token"));
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));

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
    public void getGameData_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Given - invalid token
        when(authService.getUserByToken(cleanToken)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/games/data")
                .param("lobbyId", lobbyId.toString())
                .header("Authorization", validToken)
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
    public void getGameData_ServiceException_ReturnsInternalServerError() throws Exception {
        // Given - service throws exception
        when(lobbyService.isUserHostByToken(eq(lobbyId), eq(cleanToken))).thenReturn(true);
        when(roundCardService.getAllRoundCards()).thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/games/data")
                .param("lobbyId", lobbyId.toString())
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
