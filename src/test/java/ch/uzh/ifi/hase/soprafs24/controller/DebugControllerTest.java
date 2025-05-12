package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DebugControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private RoundCardService roundCardService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private DebugController debugController;

    private GameService.GameState mockGameState;
    private GameService.GameState.GuessScreenAttributes mockGuessScreenAttributes;
    private List<RoundCardDTO> mockRoundCards;
    private String validToken = "valid-token";
    private String playerToken = "player-token";
    private Long gameId = 1L;
    private String roundCardId = "card123";
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create mock game state
        mockGameState = mock(GameService.GameState.class);
        mockGuessScreenAttributes = mock(GameService.GameState.GuessScreenAttributes.class);

        when(mockGameState.getCurrentRound()).thenReturn(1);
        when(mockGameState.getCurrentScreen()).thenReturn("GUESS");
        when(mockGameState.getRoundCardSubmitter()).thenReturn("submitter");
        when(mockGameState.getActiveRoundCard()).thenReturn("activeCard");
        when(mockGameState.getCurrentTurnPlayerToken()).thenReturn("playerToken");
        when(mockGameState.getGuessScreenAttributes()).thenReturn(mockGuessScreenAttributes);

        when(mockGuessScreenAttributes.getTime()).thenReturn(60);
        when(mockGuessScreenAttributes.getLatitude()).thenReturn(47.3769);
        when(mockGuessScreenAttributes.getLongitude()).thenReturn(8.5417);

        // Create mock round cards
        mockRoundCards = new ArrayList<>();
        RoundCardDTO card = new RoundCardDTO();
        card.setId(roundCardId);
//        card.setTitle("Test Card");
        mockRoundCards.add(card);

        // Create mock user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setToken(validToken);

        // Configure mocks
        when(gameService.getGameState(gameId)).thenReturn(mockGameState);
        when(authService.getUserByToken(validToken)).thenReturn(testUser);
        when(roundCardService.getPlayerRoundCardsByToken(eq(gameId), eq(playerToken))).thenReturn(mockRoundCards);
    }

    @Test
    void getGameState_Success() {
        // Test the endpoint
        ResponseEntity<Object> response = debugController.getGameState(gameId);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();

        // Verify the contents
        assertEquals(1, responseBody.get("currentRound"));
        assertEquals("GUESS", responseBody.get("currentScreen"));
        assertEquals("submitter", responseBody.get("roundCardSubmitter"));
        assertEquals("activeCard", responseBody.get("activeRoundCard"));
        assertEquals("playerToken", responseBody.get("currentTurnPlayerToken"));

        assertTrue(responseBody.get("guessScreenAttributes") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> guessAttrs = (Map<String, Object>) responseBody.get("guessScreenAttributes");
        assertEquals(60, guessAttrs.get("time"));
        assertEquals(47.3769, guessAttrs.get("latitude"));
        assertEquals(8.5417, guessAttrs.get("longitude"));

        // Verify interactions
        verify(gameService, times(1)).getGameState(gameId);
    }

    @Test
    void getGameState_GameNotFound() {
        // Setup mock for game not found
        when(gameService.getGameState(2L)).thenReturn(null);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.getGameState(2L);

        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Game not found", responseBody.get("error"));

        // Verify interactions
        verify(gameService, times(1)).getGameState(2L);
    }

    @Test
    void getGameState_InternalServerError() {
        // Setup mock to throw exception
        when(gameService.getGameState(gameId)).thenThrow(new RuntimeException("Test exception"));

        // Test the endpoint
        ResponseEntity<Object> response = debugController.getGameState(gameId);

        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Test exception", responseBody.get("error"));
    }

    @Test
    void getPlayerCards_Success() {
        // Test the endpoint
        ResponseEntity<Object> response = debugController.getPlayerCards(gameId, playerToken, "Bearer " + validToken);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue(responseBody.get("roundCards") instanceof List);

        @SuppressWarnings("unchecked")
        List<RoundCardDTO> roundCards = (List<RoundCardDTO>) responseBody.get("roundCards");
        assertEquals(1, roundCards.size());
        assertEquals(roundCardId, roundCards.get(0).getId());

        // Verify interactions
        verify(authService, times(1)).getUserByToken(validToken);
        verify(roundCardService, times(1)).getPlayerRoundCardsByToken(gameId, playerToken);
    }

    @Test
    void getPlayerCards_InvalidToken() {
        // Setup mock for invalid token
        when(authService.getUserByToken("invalid-token")).thenThrow(new RuntimeException("Invalid token"));

        // Test the endpoint
        ResponseEntity<Object> response = debugController.getPlayerCards(gameId, playerToken, "Bearer invalid-token");

        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Invalid token", responseBody.get("error"));
    }

    @Test
    void selectRoundCard_Success() {
        // Setup mock for coordinates - using LatLngDTO as shown in GameService
        GoogleMapsService.LatLngDTO coordinates = new GoogleMapsService.LatLngDTO(47.3769, 8.5417);
        when(gameService.startRound(eq(gameId), any(RoundCardDTO.class))).thenReturn(coordinates);

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.selectRoundCard(gameId, payload, "Bearer " + validToken);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("Round card selected successfully", responseBody.get("message"));

        // Verify interactions
        verify(authService, times(1)).getUserByToken(validToken);
        verify(roundCardService, times(1)).getPlayerRoundCardsByToken(gameId, playerToken);
        verify(gameService, times(1)).startRound(eq(gameId), any(RoundCardDTO.class));
        verify(gameService, times(1)).sendGameStateToAll(gameId);
    }

    @Test
    void selectRoundCard_MissingFields() {
        // Prepare request payload with missing fields
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        // Missing playerToken

        // Test the endpoint
        ResponseEntity<Object> response = debugController.selectRoundCard(gameId, payload, "Bearer " + validToken);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("roundCardId and playerToken are required", responseBody.get("error"));
    }

    @Test
    void selectRoundCard_GameNotFound() {
        // Setup mock for game not found
        when(gameService.getGameState(2L)).thenReturn(null);

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.selectRoundCard(2L, payload, "Bearer " + validToken);

        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Game not found", responseBody.get("error"));
    }

    @Test
    void selectRoundCard_CardNotFound() {
        // Setup mock for empty round cards
        when(roundCardService.getPlayerRoundCardsByToken(gameId, playerToken)).thenReturn(new ArrayList<>());

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.selectRoundCard(gameId, payload, "Bearer " + validToken);

        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Round card not found", responseBody.get("error"));
    }

    @Test
    void forceRoundCardSelection_Success() {
        // Setup mock for coordinates - using LatLngDTO instead of GuessScreenAttributes
        GoogleMapsService.LatLngDTO coordinates = new GoogleMapsService.LatLngDTO(47.3769, 8.5417);
        when(gameService.startRound(eq(gameId), any(RoundCardDTO.class))).thenReturn(coordinates);

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.forceRoundCardSelection(gameId, payload);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("Round card forced", responseBody.get("message"));

        assertTrue(responseBody.get("coordinates") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> coordMap = (Map<String, Object>) responseBody.get("coordinates");
        assertEquals(47.3769, coordMap.get("latitude"));
        assertEquals(8.5417, coordMap.get("longitude"));

        // Verify interactions
        verify(roundCardService, times(1)).getPlayerRoundCardsByToken(gameId, playerToken);
        verify(gameService, times(1)).startRound(eq(gameId), any(RoundCardDTO.class));
        verify(gameService, times(1)).sendGameStateToAll(gameId);
        verify(mockGameState, times(1)).setActiveRoundCard(roundCardId);
    }

    @Test
    void forceRoundCardSelection_MissingFields() {
        // Prepare request payload with missing fields
        Map<String, String> payload = new HashMap<>();
        // Missing both fields

        // Test the endpoint
        ResponseEntity<Object> response = debugController.forceRoundCardSelection(gameId, payload);

        // Verify the response
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("playerToken and roundCardId are required", responseBody.get("error"));
    }

    @Test
    void forceRoundCardSelection_GameNotFound() {
        // Setup mock for game not found
        when(gameService.getGameState(2L)).thenReturn(null);

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.forceRoundCardSelection(2L, payload);

        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Game not found", responseBody.get("error"));
    }

    @Test
    void forceRoundCardSelection_CardNotFound() {
        // Setup mock for empty round cards
        when(roundCardService.getPlayerRoundCardsByToken(gameId, playerToken)).thenReturn(new ArrayList<>());

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.forceRoundCardSelection(gameId, payload);

        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Round card not found", responseBody.get("error"));
    }

    @Test
    void forceRoundCardSelection_Exception() {
        // Setup mock to throw exception
        when(gameService.startRound(eq(gameId), any(RoundCardDTO.class))).thenThrow(new RuntimeException("Test exception"));

        // Prepare request payload
        Map<String, String> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);
        payload.put("playerToken", playerToken);

        // Test the endpoint
        ResponseEntity<Object> response = debugController.forceRoundCardSelection(gameId, payload);

        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Test exception", responseBody.get("error"));
    }
}