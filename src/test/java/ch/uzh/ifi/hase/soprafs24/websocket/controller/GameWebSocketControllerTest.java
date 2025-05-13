package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.*;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.StartGameRequestDTO;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameWebSocketControllerTest {

    @InjectMocks
    private GameWebSocketController gameWebSocketController;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

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
    private UserService userService;

    @Mock
    private AuthService authService;

    @Mock
    private Principal principal;

    private User testUser;
    private User secondUser;
    private final String TEST_TOKEN = "test-token";
    private final String SECOND_TOKEN = "second-token";
    private final String TEST_USERNAME = "testUser";
    private final String SECOND_USERNAME = "secondUser";
    private final Long TEST_USER_ID = 1L;
    private final Long SECOND_USER_ID = 2L;
    private final Long LOBBY_ID = 101L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setToken(TEST_TOKEN);
        UserProfile testUserProfile = new UserProfile();
        testUserProfile.setUsername(TEST_USERNAME);
        testUser.setProfile(testUserProfile);

        secondUser = new User();
        secondUser.setId(SECOND_USER_ID);
        secondUser.setToken(SECOND_TOKEN);
        UserProfile secondUserProfile = new UserProfile();
        secondUserProfile.setUsername(SECOND_USERNAME);
        secondUser.setProfile(secondUserProfile);

        when(principal.getName()).thenReturn(TEST_TOKEN);
        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(testUser);
        when(authService.getUserByToken(SECOND_TOKEN)).thenReturn(secondUser);
    }

    @Test
    void startGame_Success() {
        WebSocketMessage<StartGameRequestDTO> message = new WebSocketMessage<>();
        StartGameRequestDTO requestDTO = new StartGameRequestDTO();
        message.setType("GAME_START");
        message.setPayload(requestDTO);

        List<Long> playerIds = Arrays.asList(TEST_USER_ID, SECOND_USER_ID);
        when(lobbyService.isUserHostByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(LOBBY_ID)).thenReturn(playerIds);
        when(lobbyService.getLobbyById(LOBBY_ID)).thenReturn(null); // Mock just to satisfy the method call

        when(userService.getPublicProfile(TEST_USER_ID)).thenReturn(testUser);
        when(userService.getPublicProfile(SECOND_USER_ID)).thenReturn(secondUser);

        List<RoundCardDTO> testUserCards = new ArrayList<>();
        RoundCardDTO card1 = new RoundCardDTO();
        card1.setId("world-1");
        testUserCards.add(card1);

        List<RoundCardDTO> secondUserCards = new ArrayList<>();
        RoundCardDTO card2 = new RoundCardDTO();
        card2.setId("world-2");
        secondUserCards.add(card2);

        when(roundCardService.assignRoundCardsToPlayer(TEST_TOKEN)).thenReturn(testUserCards);
        when(roundCardService.assignRoundCardsToPlayer(SECOND_TOKEN)).thenReturn(secondUserCards);

        Map<String, ActionCardDTO> actionCards = new HashMap<>();
        ActionCardDTO actionCard1 = new ActionCardDTO();
        actionCard1.setId("7choices");
        ActionCardDTO actionCard2 = new ActionCardDTO();
        actionCard2.setId("badsight");
        actionCards.put(TEST_TOKEN, actionCard1);
        actionCards.put(SECOND_TOKEN, actionCard2);

        when(gameRoundService.distributeFreshActionCardsByToken(eq(LOBBY_ID), anyList())).thenReturn(actionCards);

        GameService.GameState gameState = mock(GameService.GameState.class);
        GameService.GameState.PlayerInventory testInventory = mock(GameService.GameState.PlayerInventory.class);
        GameService.GameState.PlayerInventory secondInventory = mock(GameService.GameState.PlayerInventory.class);

        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
        when(gameState.getInventoryForPlayer(TEST_TOKEN)).thenReturn(testInventory);
        when(gameState.getInventoryForPlayer(SECOND_TOKEN)).thenReturn(secondInventory);

        gameWebSocketController.startGame(LOBBY_ID, message, principal);

        verify(lobbyService).isUserHostByToken(LOBBY_ID, TEST_TOKEN);
        verify(lobbyService).getLobbyPlayerIds(LOBBY_ID);
        verify(gameService).initializeGame(eq(LOBBY_ID), anyList(), anyString());
        verify(testInventory).setRoundCards(anyList());
        verify(testInventory).setActionCards(anyList());
        verify(messagingTemplate).convertAndSend(eq("/topic/lobby/" + LOBBY_ID + "/game"), any(WebSocketMessage.class));
        verify(gameService).sendGameStateToAll(LOBBY_ID);
        verify(lobbyService).updateLobbyStatus(LOBBY_ID, "IN_PROGRESS");
    }

    @Test
    void startGame_NotHost() {
        WebSocketMessage<StartGameRequestDTO> message = new WebSocketMessage<>();
        StartGameRequestDTO requestDTO = new StartGameRequestDTO();
        message.setType("GAME_START");
        message.setPayload(requestDTO);

        when(lobbyService.isUserHostByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.startGame(LOBBY_ID, message, principal);
        });

        verify(lobbyService).isUserHostByToken(LOBBY_ID, TEST_TOKEN);
        verify(gameService, never()).initializeGame(anyLong(), anyList(), anyString());
    }

    @Test
    void startGame_NotEnoughPlayers() {
        WebSocketMessage<StartGameRequestDTO> message = new WebSocketMessage<>();
        StartGameRequestDTO requestDTO = new StartGameRequestDTO();
        message.setType("GAME_START");
        message.setPayload(requestDTO);

        List<Long> playerIds = List.of(TEST_USER_ID); // Only one player
        when(lobbyService.isUserHostByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(true);
        when(lobbyService.getLobbyPlayerIds(LOBBY_ID)).thenReturn(playerIds);
        when(lobbyService.getLobbyById(LOBBY_ID)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.startGame(LOBBY_ID, message, principal);
        });

        verify(lobbyService).isUserHostByToken(LOBBY_ID, TEST_TOKEN);
        verify(lobbyService).getLobbyPlayerIds(LOBBY_ID);
        verify(gameService, never()).initializeGame(anyLong(), anyList(), anyString());
    }

    @Test
    void requestGameState_Success() {
        gameWebSocketController.requestGameState(LOBBY_ID, principal);

        verify(authService).getUserByToken(TEST_TOKEN);
        verify(gameService).sendGameStateToUserByToken(LOBBY_ID, TEST_TOKEN);
    }

//    @Test
//    void selectRoundCard_Success() {
//        String roundCardId = "world-123";
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("roundCardId", roundCardId);
//
//        GameService.GameState gameState = mock(GameService.GameState.class);
//        when(gameState.getCurrentScreen()).thenReturn("ROUNDCARD");
//        when(gameState.getCurrentTurnPlayerToken()).thenReturn(TEST_TOKEN);
//        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
//
//        RoundCardDTO roundCard = new RoundCardDTO();
//        roundCard.setId(roundCardId);
//        RoundCardDTO.RoundCardModifiers modifiers = new RoundCardDTO.RoundCardModifiers();
//        modifiers.setTime(45);
//        roundCard.setModifiers(modifiers);
//
//        List<RoundCardDTO> playerCards = new ArrayList<>();
//        playerCards.add(roundCard);
//        when(roundCardService.getPlayerRoundCardsByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(playerCards);
//
//        LatLngDTO coordinates = new LatLngDTO(40.7128, -74.0060);
//        when(gameService.startRound(eq(LOBBY_ID), any(RoundCardDTO.class))).thenReturn(coordinates);
//
//        gameWebSocketController.selectRoundCard(LOBBY_ID, payload, principal);
//
//        verify(authService).getUserByToken(TEST_TOKEN);
//        verify(gameService).getGameState(LOBBY_ID);
//        verify(roundCardService).getPlayerRoundCardsByToken(LOBBY_ID, TEST_TOKEN);
//        verify(gameService).startRound(eq(LOBBY_ID), any(RoundCardDTO.class));
//        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby/" + LOBBY_ID + "/game"), any(WebSocketMessage.class));
//        verify(gameService).sendGameStateToAll(LOBBY_ID);
//    }

//    @Test
//    void selectRoundCard_NotYourTurn() {
//        String roundCardId = "world-123";
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("roundCardId", roundCardId);
//
//        GameService.GameState gameState = mock(GameService.GameState.class);
//        when(gameState.getCurrentScreen()).thenReturn("ROUNDCARD");
//        when(gameState.getCurrentTurnPlayerToken()).thenReturn(SECOND_TOKEN); // Different player's turn
//        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
//
//        assertThrows(ResponseStatusException.class, () -> {
//            gameWebSocketController.selectRoundCard(LOBBY_ID, payload, principal);
//        });
//    }
//
//    @Test
//    void selectRoundCard_WrongPhase() {
//        String roundCardId = "world-123";
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("roundCardId", roundCardId);
//
//        GameService.GameState gameState = mock(GameService.GameState.class);
//        when(gameState.getCurrentScreen()).thenReturn("GUESS"); // Wrong phase
//        when(gameState.getCurrentTurnPlayerToken()).thenReturn(TEST_TOKEN);
//        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
//
//        assertThrows(ResponseStatusException.class, () -> {
//            gameWebSocketController.selectRoundCard(LOBBY_ID, payload, principal);
//        });
//    }

    @Test
    void selectRoundCard_CardNotFound() {
        String roundCardId = "world-123";
        Map<String, Object> payload = new HashMap<>();
        payload.put("roundCardId", roundCardId);

        GameService.GameState gameState = mock(GameService.GameState.class);
        when(gameState.getCurrentScreen()).thenReturn("ROUNDCARD");
        when(gameState.getCurrentTurnPlayerToken()).thenReturn(TEST_TOKEN);
        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);

        List<RoundCardDTO> playerCards = new ArrayList<>(); // Empty list - no cards
        when(roundCardService.getPlayerRoundCardsByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(playerCards);

        gameWebSocketController.selectRoundCard(LOBBY_ID, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/queue/lobby/" + LOBBY_ID + "/game"),
                any(WebSocketMessage.class)
        );
    }

    @Test
    void actionCardsComplete_Success() {
        GameService.GameState gameState = mock(GameService.GameState.class);
        GameService.GameState.GuessScreenAttributes guessAttributes = mock(GameService.GameState.GuessScreenAttributes.class);
        when(guessAttributes.getLatitude()).thenReturn(40.7128);
        when(guessAttributes.getLongitude()).thenReturn(-74.0060);
        when(guessAttributes.getTime()).thenReturn(30);

        when(gameState.getCurrentRound()).thenReturn(1);
        when(gameState.getGuessScreenAttributes()).thenReturn(guessAttributes);
        when(gameState.getPlayerInfo()).thenReturn(new HashMap<>());

        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);

        gameWebSocketController.actionCardsComplete(LOBBY_ID, principal);

        verify(gameService).startGuessingPhase(LOBBY_ID);
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/lobby/" + LOBBY_ID + "/game"), any(WebSocketMessage.class));
        verify(gameService).sendGameStateToAll(LOBBY_ID);
    }

//    @Test
//    void roundTimeExpired_Success() {
//        GameService.GameState gameState = mock(GameService.GameState.class);
//        when(gameState.getCurrentScreen()).thenReturn("GUESS");
//        when(gameState.getStatus()).thenReturn(GameService.GameStatus.WAITING_FOR_GUESSES);
//        when(gameState.getPlayerGuesses()).thenReturn(new HashMap<>());
//
//        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
//        when(gameService.getPlayerTokens(LOBBY_ID)).thenReturn(Arrays.asList(TEST_TOKEN, SECOND_TOKEN));
//        when(gameService.hasPlayerSubmittedGuess(LOBBY_ID, TEST_TOKEN)).thenReturn(false);
//        when(gameService.hasPlayerSubmittedGuess(LOBBY_ID, SECOND_TOKEN)).thenReturn(true);
//        when(gameService.areAllGuessesSubmitted(LOBBY_ID)).thenReturn(true);
//        when(gameService.determineRoundWinner(LOBBY_ID)).thenReturn(TEST_TOKEN);
//
//        gameWebSocketController.roundTimeExpired(LOBBY_ID, principal);
//
//        verify(gameService).registerGuessByToken(eq(LOBBY_ID), eq(TEST_TOKEN), anyDouble(), anyDouble());
//        verify(gameService).determineRoundWinner(LOBBY_ID);
//        verify(messagingTemplate).convertAndSend(eq("/topic/lobby/" + LOBBY_ID + "/game"), Optional.ofNullable(any()));
//        verify(gameService).sendGameStateToAll(LOBBY_ID);
//    }
//
//    @Test
//    void playActionCard_Success() {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("actionCardId", "7choices");
//        payload.put("targetUsername", null);
//
//        GameService.GameState gameState = mock(GameService.GameState.class);
//        Map<String, GameService.GameState.PlayerInfo> playerInfoMap = new HashMap<>();
//        GameService.GameState.PlayerInfo playerInfo = mock(GameService.GameState.PlayerInfo.class);
//        when(playerInfo.getUsername()).thenReturn(TEST_USERNAME);
//        playerInfoMap.put(TEST_TOKEN, playerInfo);
//
//        when(gameState.getPlayerInfo()).thenReturn(playerInfoMap);
//        when(gameService.getGameState(LOBBY_ID)).thenReturn(gameState);
//
//        when(gameService.isCardPlayedInCurrentRound(LOBBY_ID, TEST_TOKEN)).thenReturn(false);
//        when(actionCardService.isValidActionCard("7choices")).thenReturn(true);
//
//        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
//        effectDTO.setEffectType("CONTINENT_HINT");
//        when(actionCardService.processActionCardForGame(eq(LOBBY_ID), eq(TEST_TOKEN), eq("7choices"), isNull())).thenReturn(effectDTO);
//
//        ActionCardDTO newCard = new ActionCardDTO();
//        newCard.setId("newCard");
//        when(gameRoundService.replacePlayerActionCardByToken(LOBBY_ID, TEST_TOKEN)).thenReturn(newCard);
//
//        when(gameService.getPlayerTokens(LOBBY_ID)).thenReturn(List.of(TEST_TOKEN));
//        when(gameService.getPlayedCardCount(LOBBY_ID)).thenReturn(1);
//
//        gameWebSocketController.playActionCard(LOBBY_ID, payload, principal);
//
//        verify(gameService).markCardPlayedThisRound(eq(LOBBY_ID), eq(TEST_TOKEN), eq("7choices"), isNull());
//        verify(actionCardService).processActionCardForGame(eq(LOBBY_ID), eq(TEST_TOKEN), eq("7choices"), isNull());
//        verify(gameRoundService).replacePlayerActionCardByToken(LOBBY_ID, TEST_TOKEN);
//        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/lobby/" + LOBBY_ID + "/game"), any(WebSocketMessage.class));
//        verify(messagingTemplate).convertAndSendToUser(
//                eq(TEST_TOKEN),
//                eq("/queue/lobby/" + LOBBY_ID + "/game/action-card"),
//                any(WebSocketMessage.class)
//        );
//    }

    @Test
    void playActionCard_AlreadyPlayedThisRound() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "7choices");

        when(gameService.isCardPlayedInCurrentRound(LOBBY_ID, TEST_TOKEN)).thenReturn(true);

        gameWebSocketController.playActionCard(LOBBY_ID, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/queue/lobby/" + LOBBY_ID + "/game"),
                any(WebSocketMessage.class)
        );
        verify(gameService, never()).markCardPlayedThisRound(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void playActionCard_InvalidCardId() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("actionCardId", "invalidCard");

        when(gameService.isCardPlayedInCurrentRound(LOBBY_ID, TEST_TOKEN)).thenReturn(false);
        when(actionCardService.isValidActionCard("invalidCard")).thenReturn(false);

        gameWebSocketController.playActionCard(LOBBY_ID, payload, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq(TEST_TOKEN),
                eq("/queue/lobby/" + LOBBY_ID + "/game"),
                any(WebSocketMessage.class)
        );
        verify(gameService, never()).markCardPlayedThisRound(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    void handleGuess_Success() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("latitude", 40.7128);
        payload.put("longitude", -74.0060);

        gameWebSocketController.handleGuess(LOBBY_ID, payload, principal);

        verify(gameService).registerGuessByToken(LOBBY_ID, TEST_TOKEN, 40.7128, -74.0060);
    }

    @Test
    void validateAuthentication_Success() {
        when(principal.getName()).thenReturn(TEST_TOKEN);

        gameWebSocketController.requestGameState(LOBBY_ID, principal);

        verify(authService).getUserByToken(TEST_TOKEN);
    }

    @Test
    void validateAuthentication_PrincipalNull() {
        Principal nullPrincipal = null;

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.requestGameState(LOBBY_ID, nullPrincipal);
        });
    }

    @Test
    void validateAuthentication_InvalidToken() {
        when(principal.getName()).thenReturn(TEST_TOKEN);
        when(authService.getUserByToken(TEST_TOKEN)).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> {
            gameWebSocketController.requestGameState(LOBBY_ID, principal);
        });
    }

    @Test
    void validateAuthentication_UserIdPrincipal() {
        when(principal.getName()).thenReturn(TEST_USER_ID.toString());
        when(userService.getPublicProfile(TEST_USER_ID)).thenReturn(testUser);

        gameWebSocketController.requestGameState(LOBBY_ID, principal);

        verify(userService).getPublicProfile(TEST_USER_ID);
    }
}