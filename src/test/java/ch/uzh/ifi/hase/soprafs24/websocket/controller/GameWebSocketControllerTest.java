package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.*;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.RoundWinnerBroadcast;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketControllerTest {

    @Mock
    private GameService gameService;

    @Mock
    private GameRoundService gameRoundService;

    @Mock
    private RoundCardService roundCardService;

    @Mock
    private ActionCardService actionCardService;

    @Mock
    private LobbyService lobbyService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private Principal principal;

    @InjectMocks
    private GameWebSocketController gameWebSocketController;

    private User testUser;
    private String validToken = "valid-token";
    private Long lobbyId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(validToken);

        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        testUser.setProfile(profile);

        when(principal.getName()).thenReturn(validToken);
        when(authService.getUserByToken(validToken)).thenReturn(testUser);
    }

    @Test
    void testValidateAuthentication_ValidToken() {
        gameWebSocketController.requestGameState(lobbyId, principal);
        verify(gameService).sendGameStateToUserByToken(lobbyId, validToken);
    }

//    @Test
//    void testValidateAuthentication_NullPrincipal() {
//        assertThrows(ResponseStatusException.class, () -> {
//            gameWebSocketController.requestGameState(lobbyId, null);
//        });
//    }
//
//    @Test
//    void testValidateAuthentication_InvalidToken() {
//        when(authService.getUserByToken(anyString())).thenReturn(null);
//
//        assertThrows(ResponseStatusException.class, () -> {
//            gameWebSocketController.requestGameState(lobbyId, principal);
//        });
//    }

    @Test
    void testStartGame_SuccessfulStart() {
        when(lobbyService.isUserHostByToken(lobbyId, validToken)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));

        User user2 = new User();
        user2.setId(2L);
        user2.setToken("token2");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("user2");
        user2.setProfile(profile2);

        when(userService.getPublicProfile(1L)).thenReturn(testUser);
        when(userService.getPublicProfile(2L)).thenReturn(user2);

        List<RoundCardDTO> roundCards = Arrays.asList(new RoundCardDTO());
        when(roundCardService.assignRoundCardsToPlayer(anyString())).thenReturn(roundCards);

        ActionCardDTO actionCard = new ActionCardDTO();
        actionCard.setId("action1");
        when(gameRoundService.distributeFreshActionCardsByToken(eq(lobbyId), anyList()))
                .thenReturn(Map.of(validToken, actionCard, "token2", actionCard));

        GameService.GameState gameState = new GameService.GameState();
        gameState.getInventoryForPlayer(validToken);
        gameState.getInventoryForPlayer("token2");
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        gameWebSocketController.startGame(lobbyId, new WebSocketMessage<>("START_GAME", null), principal);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(WebSocketMessage.class)
        );
        verify(gameService).sendGameStateToAll(lobbyId);
        verify(lobbyService).updateLobbyStatus(lobbyId, "IN_PROGRESS");
    }

    @Test
    void testStartGame_NotHost() {
        when(lobbyService.isUserHostByToken(lobbyId, validToken)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.startGame(lobbyId, new WebSocketMessage<>("START_GAME", null), principal);
        });
    }

    @Test
    void testStartGame_InsufficientPlayers() {
        when(lobbyService.isUserHostByToken(lobbyId, validToken)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L));

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.startGame(lobbyId, new WebSocketMessage<>("START_GAME", null), principal);
        });
    }

    @Test
    void testRequestGameState() {
        gameWebSocketController.requestGameState(lobbyId, principal);

        verify(gameService).sendGameStateToUserByToken(lobbyId, validToken);
    }

    @Test
    void testSelectRoundCard_Success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "round-card-1");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken(validToken);
        gameState.setCurrentScreen("ROUNDCARD");
        gameState.getInventoryForPlayer(validToken);
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        RoundCardDTO selectedCard = new RoundCardDTO();
        selectedCard.setId("round-card-1");
        RoundCardDTO.RoundCardModifiers modifiers = new RoundCardDTO.RoundCardModifiers();
        modifiers.setTime(45);
        selectedCard.setModifiers(modifiers);

        List<RoundCardDTO> playerCards = Arrays.asList(selectedCard);
        when(roundCardService.getPlayerRoundCardsByToken(lobbyId, validToken)).thenReturn(playerCards);

        LatLngDTO coordinates = new LatLngDTO(40.7128, -74.0060);
        when(gameService.startRound(lobbyId, selectedCard)).thenReturn(coordinates);

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(messagingTemplate, times(3)).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(WebSocketMessage.class)
        );
        verify(gameService).sendGameStateToAll(lobbyId);
    }

    @Test
    void testSelectRoundCard_NotPlayerTurn() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "round-card-1");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken("other-token");
        gameState.setCurrentScreen("ROUNDCARD");
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> ((WebSocketMessage<?>) msg).getType().equals("ERROR"))
        );
    }

//    @Test
//    void testPlayActionCard_Success() {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("actionCardId", "badsight");
//        payload.put("targetUsername", "user2");
//
//        GameService.GameState gameState = new GameService.GameState();
//        GameService.GameState.PlayerInfo playerInfo1 = new GameService.GameState.PlayerInfo();
//        playerInfo1.setUsername("testUser");
//        GameService.GameState.PlayerInfo playerInfo2 = new GameService.GameState.PlayerInfo();
//        playerInfo2.setUsername("user2");
//
//        gameState.getPlayerInfo().put("token1", playerInfo1);
//        gameState.getPlayerInfo().put("token2", playerInfo2);
//
//        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
//        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
//        when(actionCardService.isValidActionCard("badsight")).thenReturn(true);
//
//        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
//        when(actionCardService.processActionCardForGame(eq(lobbyId), eq(validToken), eq("badsight"), eq("token2")))
//                .thenReturn(effectDTO);
//
//        ActionCardDTO newCard = new ActionCardDTO();
//        newCard.setId("newcard");
//        when(gameRoundService.replacePlayerActionCardByToken(lobbyId, validToken)).thenReturn(newCard);
//
//        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken, "token2"));
//        when(gameService.getPlayedCardCount(lobbyId)).thenReturn(1);
//
//        gameWebSocketController.playActionCard(lobbyId, payload, principal);
//
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby/" + lobbyId + "/game"),
//                any(WebSocketMessage.class)
//        );
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(validToken),
//                eq("/queue/lobby/" + lobbyId + "/game/action-card"),
//                any(WebSocketMessage.class)
//        );
//        verify(gameService).sendGameStateToAll(lobbyId);
//    }

    @Test
    void testPlayActionCard_AlreadyPlayed() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "badsight");

        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(true);

        gameWebSocketController.playActionCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testHandleGuess() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("latitude", 40.7128);
        payload.put("longitude", -74.0060);

        gameWebSocketController.handleGuess(lobbyId, payload, principal);

        verify(gameService).registerGuessByToken(lobbyId, validToken, 40.7128, -74.0060);
    }

    @Test
    void testRoundTimeExpired_Success() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentScreen("GUESS");
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_GUESSES);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);

        gameState.getPlayerGuesses().put(validToken, 100);
        gameState.getPlayerGuesses().put("token2", 200);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken, "token2"));
        when(gameService.hasPlayerSubmittedGuess(lobbyId, validToken)).thenReturn(true);
        when(gameService.hasPlayerSubmittedGuess(lobbyId, "token2")).thenReturn(true);
        when(gameService.areAllGuessesSubmitted(lobbyId)).thenReturn(true);
        when(gameService.determineRoundWinner(lobbyId)).thenReturn(validToken);

        gameWebSocketController.roundTimeExpired(lobbyId, principal);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(RoundWinnerBroadcast.class)
        );
    }

    @Test
    void testActionCardsComplete() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentRound(1);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);
        gameState.getGuessScreenAttributes().setTime(30);

        GameService.GameState.PlayerInfo playerInfo = new GameService.GameState.PlayerInfo();
        playerInfo.setActiveActionCards(Arrays.asList("7choices"));
        Map<String, GameService.GameState.PlayerInfo> playerInfoMap = new HashMap<>();
        playerInfoMap.put(validToken, playerInfo);

        gameState.getPlayerInfo().putAll(playerInfoMap);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(actionCardService.getContinent(anyDouble(), anyDouble())).thenReturn("North America");

        gameWebSocketController.actionCardsComplete(lobbyId, principal);

        verify(gameService).startGuessingPhase(lobbyId);
        verify(messagingTemplate, times(2)).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(WebSocketMessage.class)
        );
        verify(gameService).sendGameStateToAll(lobbyId);
    }

    @Test
    void testSelectRoundCard_CardWithTypePrefix() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "world-123");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken(validToken);
        gameState.setCurrentScreen("ROUNDCARD");
        gameState.getInventoryForPlayer(validToken);
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        RoundCardDTO actualCard = new RoundCardDTO();
        actualCard.setId("world-456");
        RoundCardDTO.RoundCardModifiers modifiers = new RoundCardDTO.RoundCardModifiers();
        modifiers.setTime(30);
        actualCard.setModifiers(modifiers);

        List<RoundCardDTO> playerCards = Arrays.asList(actualCard);
        when(roundCardService.getPlayerRoundCardsByToken(lobbyId, validToken)).thenReturn(playerCards);

        LatLngDTO coordinates = new LatLngDTO(40.7128, -74.0060);
        when(gameService.startRound(lobbyId, actualCard)).thenReturn(coordinates);

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(gameService).startRound(lobbyId, actualCard);
        verify(gameService).sendGameStateToAll(lobbyId);
    }

    @Test
    void testSelectRoundCard_CardNotFound() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "nonexistent-card");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken(validToken);
        gameState.setCurrentScreen("ROUNDCARD");
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        List<RoundCardDTO> playerCards = new ArrayList<>();
        when(roundCardService.getPlayerRoundCardsByToken(lobbyId, validToken)).thenReturn(playerCards);

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testSelectRoundCard_WrongPhase() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "round-card-1");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken(validToken);
        gameState.setCurrentScreen("ACTIONCARD");
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testPlayActionCard_InvalidCardId() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "invalid-card");

        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
        when(actionCardService.isValidActionCard("invalid-card")).thenReturn(false);

        gameWebSocketController.playActionCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testPlayActionCard_ProcessingFailure() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "valid-card");

        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
        when(actionCardService.isValidActionCard("valid-card")).thenReturn(true);
        when(actionCardService.processActionCardForGame(eq(lobbyId), eq(validToken), eq("valid-card"), any()))
                .thenReturn(null);

        gameWebSocketController.playActionCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testPlayActionCard_AllPlayersCompleted() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "7choices");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentRound(1);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);
        gameState.getGuessScreenAttributes().setTime(30);

        GameService.GameState.PlayerInfo playerInfo = new GameService.GameState.PlayerInfo();
        playerInfo.setActiveActionCards(Arrays.asList("7choices"));
        Map<String, GameService.GameState.PlayerInfo> playerInfoMap = new HashMap<>();
        playerInfoMap.put(validToken, playerInfo);

        gameState.getPlayerInfo().putAll(playerInfoMap);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
        when(actionCardService.isValidActionCard("7choices")).thenReturn(true);

        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
        when(actionCardService.processActionCardForGame(eq(lobbyId), eq(validToken), eq("7choices"), any()))
                .thenReturn(effectDTO);

        ActionCardDTO newCard = new ActionCardDTO();
        when(gameRoundService.replacePlayerActionCardByToken(lobbyId, validToken)).thenReturn(newCard);

        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken));
        when(gameService.getPlayedCardCount(lobbyId)).thenReturn(1);
        when(actionCardService.getContinent(anyDouble(), anyDouble())).thenReturn("Europe");

        gameWebSocketController.playActionCard(lobbyId, payload, principal);

        verify(gameService).startGuessingPhase(lobbyId);
        verify(messagingTemplate, atLeast(2)).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(WebSocketMessage.class)
        );
    }

    @Test
    void testRoundTimeExpired_NotInGuessingPhase() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentScreen("ACTIONCARD");
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_ACTION_CARDS);
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        gameWebSocketController.roundTimeExpired(lobbyId, principal);

        verify(gameService, never()).determineRoundWinner(anyLong());
    }

//    @Test
//    void testRoundTimeExpired_WithDefaultGuesses() {
//        GameService.GameState gameState = new GameService.GameState();
//        gameState.setCurrentScreen("GUESS");
//        gameState.setStatus(GameService.GameStatus.WAITING_FOR_GUESSES);
//        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
//        gameState.getGuessScreenAttributes().setLatitude(40.7128);
//        gameState.getGuessScreenAttributes().setLongitude(-74.0060);
//
//        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
//        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken, "token2"));
//        when(gameService.hasPlayerSubmittedGuess(lobbyId, validToken)).thenReturn(true);
//        when(gameService.hasPlayerSubmittedGuess(lobbyId, "token2")).thenReturn(false);
//        when(gameService.areAllGuessesSubmitted(lobbyId)).thenReturn(true);
//        when(gameService.determineRoundWinner(lobbyId)).thenReturn(validToken);
//
//        gameWebSocketController.roundTimeExpired(lobbyId, principal);
//
//        verify(gameService).registerGuessByToken(eq(lobbyId), eq("token2"), anyDouble(), anyDouble());
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby/" + lobbyId + "/game"),
//                any(RoundWinnerBroadcast.class)
//        );
//    }

    @Test
    void testStartGame_WithExceptionDuringDistribution() {
        when(lobbyService.isUserHostByToken(lobbyId, validToken)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(Arrays.asList(1L, 2L));
        when(userService.getPublicProfile(anyLong())).thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            gameWebSocketController.startGame(lobbyId, new WebSocketMessage<>("START_GAME", null), principal);
        });
    }

//    @Test
//    void testValidateAuthentication_UserIdAsPrincipal() {
//        when(principal.getName()).thenReturn("123");
//
//        User userById = new User();
//        userById.setId(123L);
//        userById.setToken("user-token");
//        when(userService.getPublicProfile(123L)).thenReturn(userById);
//        when(authService.getUserByToken("user-token")).thenReturn(userById);
//
//        gameWebSocketController.requestGameState(lobbyId, principal);
//
//        verify(gameService).sendGameStateToUserByToken(lobbyId, "user-token");
//    }
//
//    @Test
//    void testActionCardEffects_BadSight() {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("actionCardId", "badsight");
//        payload.put("targetUsername", "user2");
//
//        GameService.GameState gameState = new GameService.GameState();
//        GameService.GameState.PlayerInfo playerInfo1 = new GameService.GameState.PlayerInfo();
//        playerInfo1.setUsername("testUser");
//        GameService.GameState.PlayerInfo playerInfo2 = new GameService.GameState.PlayerInfo();
//        playerInfo2.setUsername("user2");
//        gameState.getPlayerInfo().put("token1", playerInfo1);
//        gameState.getPlayerInfo().put("token2", playerInfo2);
//
//        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
//        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
//        when(actionCardService.isValidActionCard("badsight")).thenReturn(true);
//
//        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
//        when(actionCardService.processActionCardForGame(eq(lobbyId), eq(validToken), eq("badsight"), eq("token2")))
//                .thenReturn(effectDTO);
//
//        ActionCardDTO newCard = new ActionCardDTO();
//        when(gameRoundService.replacePlayerActionCardByToken(lobbyId, validToken)).thenReturn(newCard);
//        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken, "token2"));
//        when(gameService.getPlayedCardCount(lobbyId)).thenReturn(1);
//
//        gameWebSocketController.playActionCard(lobbyId, payload, principal);
//
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby/" + lobbyId + "/game"),
//                any(WebSocketMessage.class)
//        );
//    }

    @Test
    void testRoundTimeExpired_ProcessRoundCompletion() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentScreen("GUESS");
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_GUESSES);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);
        gameState.setCurrentRoundCardPlayer(validToken);
        gameState.setCurrentRoundCardId("round-card-1");

        gameState.getPlayerGuesses().put(validToken, 100);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken));
        when(gameService.hasPlayerSubmittedGuess(lobbyId, validToken)).thenReturn(true);
        when(gameService.areAllGuessesSubmitted(lobbyId)).thenReturn(true);
        when(gameService.determineRoundWinner(lobbyId)).thenReturn(validToken);

        gameWebSocketController.roundTimeExpired(lobbyId, principal);

        verify(gameService).prepareNextRound(lobbyId, validToken);
        assertNull(gameState.getCurrentRoundCardPlayer());
        assertNull(gameState.getCurrentRoundCardId());
        verify(gameService, times(2)).sendGameStateToAll(lobbyId);
    }

    @Test
    void testRoundTimeExpired_ProcessRoundCompletion_NoGameState() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentScreen("GUESS");
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_GUESSES);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);

        gameState.getPlayerGuesses().put(validToken, 100);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken));
        when(gameService.hasPlayerSubmittedGuess(lobbyId, validToken)).thenReturn(true);
        when(gameService.areAllGuessesSubmitted(lobbyId)).thenReturn(true);
        when(gameService.determineRoundWinner(lobbyId)).thenReturn(validToken);

        doAnswer(invocation -> {
            when(gameService.getGameState(lobbyId)).thenReturn(null);
            return null;
        }).when(gameService).prepareNextRound(lobbyId, validToken);

        gameWebSocketController.roundTimeExpired(lobbyId, principal);

        verify(gameService).prepareNextRound(lobbyId, validToken);
        verify(gameService, times(1)).sendGameStateToAll(lobbyId);
    }

    @Test
    void testActionCardsComplete_Exception() {
        doThrow(new RuntimeException("Failed to start")).when(gameService).startGuessingPhase(lobbyId);

        gameWebSocketController.actionCardsComplete(lobbyId, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> ((WebSocketMessage<?>) msg).getType().equals("ERROR"))
        );
    }

    @Test
    void testSelectRoundCard_StartRoundException() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", "round-card-1");

        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentTurnPlayerToken(validToken);
        gameState.setCurrentScreen("ROUNDCARD");
        gameState.getInventoryForPlayer(validToken);
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        RoundCardDTO selectedCard = new RoundCardDTO();
        selectedCard.setId("round-card-1");
        RoundCardDTO.RoundCardModifiers modifiers = new RoundCardDTO.RoundCardModifiers();
        modifiers.setTime(30);
        selectedCard.setModifiers(modifiers);

        List<RoundCardDTO> playerCards = Arrays.asList(selectedCard);
        when(roundCardService.getPlayerRoundCardsByToken(lobbyId, validToken)).thenReturn(playerCards);

        when(gameService.startRound(lobbyId, selectedCard)).thenThrow(new RuntimeException("Error fetching coordinates"));

        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(validToken),
                eq("/queue/lobby/" + lobbyId + "/game"),
                argThat(msg -> {
                    WebSocketMessage<?> wsMsg = (WebSocketMessage<?>) msg;
                    return "ERROR".equals(wsMsg.getType());
                })
        );
    }

    @Test
    void testValidateAuthentication_BearerTokenFormat() {
        when(principal.getName()).thenReturn("Bearer " + validToken);
        when(authService.getUserByToken(validToken)).thenReturn(testUser);

        gameWebSocketController.requestGameState(lobbyId, principal);

        verify(gameService).sendGameStateToUserByToken(lobbyId, validToken);
    }

//    @Test
//    void testPlayActionCard_NullTargetUsername() {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("actionCardId", "7choices");
//        payload.put("targetUsername", null);
//
//        when(gameService.isCardPlayedInCurrentRound(lobbyId, validToken)).thenReturn(false);
//        when(actionCardService.isValidActionCard("7choices")).thenReturn(true);
//
//        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
//        when(actionCardService.processActionCardForGame(eq(lobbyId), eq(validToken), eq("7choices"), isNull()))
//                .thenReturn(effectDTO);
//
//        ActionCardDTO newCard = new ActionCardDTO();
//        when(gameRoundService.replacePlayerActionCardByToken(lobbyId, validToken)).thenReturn(newCard);
//        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken));
//        when(gameService.getPlayedCardCount(lobbyId)).thenReturn(1);
//
//        GameService.GameState gameState = new GameService.GameState();
//        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
//        gameState.getGuessScreenAttributes().setLatitude(40.7128);
//        gameState.getGuessScreenAttributes().setLongitude(-74.0060);
//        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
//        when(actionCardService.getContinent(anyDouble(), anyDouble())).thenReturn("Europe");
//
//        gameWebSocketController.playActionCard(lobbyId, payload, principal);
//
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby/" + lobbyId + "/game"),
//                any(WebSocketMessage.class)
//        );
//    }
//
//    @Test
//    void testSelectRoundCard_NoModifiers() {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("roundCardId", "round-card-1");
//
//        GameService.GameState gameState = new GameService.GameState();
//        gameState.setCurrentTurnPlayerToken(validToken);
//        gameState.setCurrentScreen("ROUNDCARD");
//        gameState.getInventoryForPlayer(validToken);
//        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
//
//        RoundCardDTO selectedCard = new RoundCardDTO();
//        selectedCard.setId("round-card-1");
//        selectedCard.setModifiers(null);
//
//        List<RoundCardDTO> playerCards = Arrays.asList(selectedCard);
//        when(roundCardService.getPlayerRoundCardsByToken(lobbyId, validToken)).thenReturn(playerCards);
//
//        LatLngDTO coordinates = new LatLngDTO(40.7128, -74.0060);
//        when(gameService.startRound(lobbyId, selectedCard)).thenReturn(coordinates);
//
//        gameWebSocketController.selectRoundCard(lobbyId, payload, principal);
//
//        verify(messagingTemplate).convertAndSend(
//                eq("/topic/lobby/" + lobbyId + "/game"),
//                any(WebSocketMessage.class)
//        );
//    }

    @Test
    void testRoundTimeExpired_NoWinner() {
        GameService.GameState gameState = new GameService.GameState();
        gameState.setCurrentScreen("GUESS");
        gameState.setStatus(GameService.GameStatus.WAITING_FOR_GUESSES);
        gameState.setGuessScreenAttributes(new GameService.GameState.GuessScreenAttributes());
        gameState.getGuessScreenAttributes().setLatitude(40.7128);
        gameState.getGuessScreenAttributes().setLongitude(-74.0060);

        gameState.getPlayerGuesses().put(validToken, 100);

        when(gameService.getGameState(lobbyId)).thenReturn(gameState);
        when(gameService.getPlayerTokens(lobbyId)).thenReturn(Arrays.asList(validToken));
        when(gameService.hasPlayerSubmittedGuess(lobbyId, validToken)).thenReturn(true);
        when(gameService.areAllGuessesSubmitted(lobbyId)).thenReturn(true);
        when(gameService.determineRoundWinner(lobbyId)).thenReturn(null);

        gameWebSocketController.roundTimeExpired(lobbyId, principal);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/lobby/" + lobbyId + "/game"),
                any(RoundWinnerBroadcast.class)
        );
    }
//
//    @Test
//    void testValidateAuthentication_UserIdNotFound() {
//        when(principal.getName()).thenReturn("999");
//        when(userService.getPublicProfile(999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        assertThrows(ResponseStatusException.class, () -> {
//            gameWebSocketController.requestGameState(lobbyId, principal);
//        });
//    }

    @Test
    void testPlayActionCard_TargetUserNotFound() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "badsight");
        payload.put("targetUsername", "nonexistentUser");

        GameService.GameState gameState = new GameService.GameState();
        GameService.GameState.PlayerInfo playerInfo = new GameService.GameState.PlayerInfo();
        playerInfo.setUsername("testUser");
        gameState.getPlayerInfo().put("token1", playerInfo);
        when(gameService.getGameState(lobbyId)).thenReturn(gameState);

        assertThrows(IllegalArgumentException.class, () -> {
            gameWebSocketController.playActionCard(lobbyId, payload, principal);
        });
    }

    @Test
    void testStartGame_EmptyLobby() {
        when(lobbyService.isUserHostByToken(lobbyId, validToken)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(lobbyId)).thenReturn(new ArrayList<>());

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.startGame(lobbyId, new WebSocketMessage<>("START_GAME", null), principal);
        });
    }
}