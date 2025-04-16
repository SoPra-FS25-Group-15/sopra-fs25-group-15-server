package ch.uzh.ifi.hase.soprafs24.websocket.controller;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.ActionCardRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardEffectDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardPlayDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketActionCardHandlerTest {

    private WebSocketActionCardHandler webSocketActionCardHandler;
    private ActionCardService actionCardService;
    private UserService userService;
    private GameRepository gameRepository;
    private UserRepository userRepository;
    private ActionCardRepository actionCardRepository;
    private SimpMessagingTemplate messagingTemplate;

    private User testUser;
    private User targetUser;
    private Game testGame;
    private ActionCard testActionCard;
    private String testToken = "test-token";
    private Long gameId = 1L;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        actionCardService = mock(ActionCardService.class);
        userService = mock(UserService.class);
        gameRepository = mock(GameRepository.class);
        userRepository = mock(UserRepository.class);
        actionCardRepository = mock(ActionCardRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        // Create the handler with mocked dependencies
        webSocketActionCardHandler = new WebSocketActionCardHandler(
                actionCardService,
                userService,
                gameRepository,
                userRepository,
                actionCardRepository,
                messagingTemplate
        );

        // Set up test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(testToken);
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("testUser");
        testUser.setProfile(userProfile);

        targetUser = new User();
        targetUser.setId(2L);
        UserProfile targetUserProfile = new UserProfile();
        targetUserProfile.setUsername("targetUser");
        targetUser.setProfile(targetUserProfile);

        testGame = new Game();
        testGame.setId(gameId);
        Set<User> players = new HashSet<>();
        players.add(testUser);
        players.add(targetUser);
        testGame.setPlayers(players);

        testActionCard = new ActionCard();
        testActionCard.setId(1L);
        testActionCard.setType(ActionCardType.POWERUP);
        testActionCard.setOwner(testUser);

        // Set up mock responses
        when(userService.getUserByToken(testToken)).thenReturn(testUser);
        when(userService.findByUsername("targetUser")).thenReturn(targetUser);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(testGame));
        when(actionCardRepository.findById(1L)).thenReturn(Optional.of(testActionCard));
    }

    @Test
    void handleActionCardSubmit_validCardPlay() {
        // Create a punishment card for testing
        ActionCard punishmentCard = new ActionCard();
        punishmentCard.setId(2L);
        punishmentCard.setType(ActionCardType.PUNISHMENT);
        punishmentCard.setOwner(testUser);
        when(actionCardRepository.findById(2L)).thenReturn(Optional.of(punishmentCard));

        // Set up action card effect response
        ActionCardEffectDTO effectDTO = new ActionCardEffectDTO();
        effectDTO.setEffectValue("Player testUser played a punishment card against targetUser");
        when(actionCardService.playActionCard(eq(gameId), eq(testUser.getId()), any(ActionCardPlayDTO.class)))
                .thenReturn(effectDTO);

        // Create the payload
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ACTION_CARD_SUBMIT");
        payload.put("token", testToken);
        payload.put("actionCardId", "2");
        payload.put("toUserName", "targetUser");

        // Call the method under test
        webSocketActionCardHandler.handleActionCardSubmit(gameId, payload);

        // Verify action card service was called with correct parameters
        ArgumentCaptor<ActionCardPlayDTO> playDTOCaptor = ArgumentCaptor.forClass(ActionCardPlayDTO.class);
        verify(actionCardService).playActionCard(eq(gameId), eq(testUser.getId()), playDTOCaptor.capture());

        ActionCardPlayDTO capturedDTO = playDTOCaptor.getValue();
        assertEquals(2L, capturedDTO.getActionCardId());
        assertEquals(targetUser.getId(), capturedDTO.getTargetPlayerId());

        // Verify success message was sent
        verify(messagingTemplate).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/games/" + gameId + "/action-card-result"),
                Mockito.argThat((Map<String, Object> map) ->
                        map.get("status").equals("success") &&
                                map.get("message").equals("Card played successfully") &&
                                map.get("effect").equals(effectDTO)
                )
        );
    }

    @Test
    void handleActionCardSubmit_skipOption() {
        // Create the payload for SKIP
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ACTION_CARD_SUBMIT");
        payload.put("token", testToken);
        payload.put("actionCardId", "SKIP");

        // Call the method under test
        webSocketActionCardHandler.handleActionCardSubmit(gameId, payload);

        // Verify broadcast message was sent
        verify(messagingTemplate).convertAndSend(
                eq("/topic/games/" + gameId + "/action-cards"),
                Mockito.argThat((Map<String, Object> map) ->
                        map.get("type").equals("SKIP") &&
                                map.get("playerId").equals(testUser.getId().toString()) &&
                                map.get("playerName").equals(testUser.getProfile().getUsername())
                )
        );

        // Verify success message was sent to user
        verify(messagingTemplate).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/games/" + gameId + "/action-card-result"),
                Mockito.argThat((Map<String, Object> map) ->
                        map.get("status").equals("success") &&
                                map.get("message").equals("Skipped playing an action card")
                )
        );
    }

    @Test
    void handleActionCardSubmit_invalidToken() {
        // Setup invalid token
        when(userService.getUserByToken("invalid-token")).thenReturn(null);

        // Create the payload with invalid token
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ACTION_CARD_SUBMIT");
        payload.put("token", "invalid-token");
        payload.put("actionCardId", "1");

        // Call the method under test
        webSocketActionCardHandler.handleActionCardSubmit(gameId, payload);

        // Verify error handling (no error message should be sent since we can't identify the user)
        verify(actionCardService, never()).playActionCard(any(), any(), any());
    }

    @Test
    void handleActionCardSubmit_cardNotOwnedByUser() {
        // Update card ownership
        testActionCard.setOwner(targetUser);

        // Create the payload
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ACTION_CARD_SUBMIT");
        payload.put("token", testToken);
        payload.put("actionCardId", "1");

        // Call the method under test
        webSocketActionCardHandler.handleActionCardSubmit(gameId, payload);

        // Verify error message was sent
        verify(messagingTemplate).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/errors"),
                Mockito.argThat((Map<String, Object> map) ->
                        map.get("status").equals("error") &&
                                map.get("message").equals("You don't have this card in your hand")
                )
        );

        // Verify action card service was not called
        verify(actionCardService, never()).playActionCard(any(), any(), any());
    }

    @Test
    void handleActionCardSubmit_punishmentCardNoTarget() {
        // Create a punishment card for testing
        ActionCard punishmentCard = new ActionCard();
        punishmentCard.setId(2L);
        punishmentCard.setType(ActionCardType.PUNISHMENT);
        punishmentCard.setOwner(testUser);
        when(actionCardRepository.findById(2L)).thenReturn(Optional.of(punishmentCard));

        // Create the payload without target player
        Map<String, String> payload = new HashMap<>();
        payload.put("type", "ACTION_CARD_SUBMIT");
        payload.put("token", testToken);
        payload.put("actionCardId", "2");
        // No toUserName field

        // Call the method under test
        webSocketActionCardHandler.handleActionCardSubmit(gameId, payload);

        // Verify error message was sent
        verify(messagingTemplate).convertAndSendToUser(
                eq(testUser.getId().toString()),
                eq("/queue/errors"),
                Mockito.argThat((Map<String, Object> map) ->
                        map.get("status").equals("error") &&
                                map.get("message").equals("Please select a player this card should be applied to")
                )
        );

        // Verify action card service was not called
        verify(actionCardService, never()).playActionCard(any(), any(), any());
    }
}