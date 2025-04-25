package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;
import ch.uzh.ifi.hase.soprafs24.service.GoogleMapsService.LatLngDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class GameRoundServiceTest {

    @InjectMocks
    private GameRoundService gameRoundService;

    @Mock
    private ActionCardService actionCardService;

    @Mock
    private GameService gameService;

    @Mock
    private GoogleMapsService googleMapsService;

    @Mock
    private AuthService authService;

    @Mock
    private ActionCardMapper actionCardMapper;

    private final Long gameId = 1L;
    private final List<String> playerTokens = List.of("player1-token", "player2-token", "player3-token");

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartGame() {
        // Call the method
        gameRoundService.startGame(gameId, playerTokens);

        // Start another round to verify the round counter is correctly initialized
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(new LatLngDTO(10.0, 20.0));
        GameRoundService.RoundData roundData = gameRoundService.startNextRound(gameId, playerTokens);

        // Verify the round number is 1 (first round)
        assertEquals(1, roundData.getRoundNumber());
    }

    @Test
    public void testStartNextRound() {
        // Setup
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(new LatLngDTO(10.0, 20.0));
        gameRoundService.startGame(gameId, playerTokens);

        // Call the method
        GameRoundService.RoundData roundData = gameRoundService.startNextRound(gameId, playerTokens);

        // Verify
        assertNotNull(roundData);
        assertEquals(1, roundData.getRoundNumber());
        assertEquals(10.0, roundData.getLatitude());
        assertEquals(20.0, roundData.getLongitude());
        assertEquals(1, roundData.getGuesses());

        // Call it again to verify the counter increases
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(new LatLngDTO(30.0, 40.0));
        roundData = gameRoundService.startNextRound(gameId, playerTokens);
        assertEquals(2, roundData.getRoundNumber());
        assertEquals(30.0, roundData.getLatitude());
        assertEquals(40.0, roundData.getLongitude());
    }

    @Test
    public void testHasMoreRounds() {
        // Setup
        gameRoundService.startGame(gameId, playerTokens);

        // Initial state - should have more rounds
        assertTrue(gameRoundService.hasMoreRounds(gameId));

        // After round 1
        when(googleMapsService.getRandomCoordinatesOnLand()).thenReturn(new LatLngDTO(10.0, 20.0));
        gameRoundService.startNextRound(gameId, playerTokens);
        assertTrue(gameRoundService.hasMoreRounds(gameId));

        // After round 2
        gameRoundService.startNextRound(gameId, playerTokens);
        assertTrue(gameRoundService.hasMoreRounds(gameId));

        // After round 3
        gameRoundService.startNextRound(gameId, playerTokens);
        assertFalse(gameRoundService.hasMoreRounds(gameId));
    }

    @Test
    public void testDistributeFreshActionCardsByToken() {
        // Setup
        gameRoundService.startGame(gameId, playerTokens);
        
        // Create mock users and action cards
        User user1 = new User();
        user1.setToken(playerTokens.get(0));
        
        User user2 = new User();
        user2.setToken(playerTokens.get(1));
        
        User user3 = new User();
        user3.setToken(playerTokens.get(2));
        
        ActionCardDTO card1 = createMockActionCard("7choices", "powerup", "7 Choices", 
                "Reveal the continent of the target location.");
        ActionCardDTO card2 = createMockActionCard("badsight", "punishment", "Bad Sight", 
                "A player of your choice has their screen blurred for the first 15 seconds of the round.");
        ActionCardDTO card3 = createMockActionCard("7choices", "powerup", "7 Choices", 
                "Reveal the continent of the target location.");
        
        // Setup mock GameState and player inventories
        GameService.GameState gameState = mock(GameService.GameState.class);
        GameService.GameState.PlayerInventory inventory1 = mock(GameService.GameState.PlayerInventory.class);
        GameService.GameState.PlayerInventory inventory2 = mock(GameService.GameState.PlayerInventory.class);
        GameService.GameState.PlayerInventory inventory3 = mock(GameService.GameState.PlayerInventory.class);
        
        Map<String, GameService.GameState.PlayerInfo> playerInfoMap = new HashMap<>();
        GameService.GameState.PlayerInfo playerInfo1 = mock(GameService.GameState.PlayerInfo.class);
        GameService.GameState.PlayerInfo playerInfo2 = mock(GameService.GameState.PlayerInfo.class);
        GameService.GameState.PlayerInfo playerInfo3 = mock(GameService.GameState.PlayerInfo.class);
        
        playerInfoMap.put(playerTokens.get(0), playerInfo1);
        playerInfoMap.put(playerTokens.get(1), playerInfo2);
        playerInfoMap.put(playerTokens.get(2), playerInfo3);
        
        // Setup mocks
        when(authService.getUserByToken(playerTokens.get(0))).thenReturn(user1);
        when(authService.getUserByToken(playerTokens.get(1))).thenReturn(user2);
        when(authService.getUserByToken(playerTokens.get(2))).thenReturn(user3);
        
        when(actionCardService.drawRandomCard())
                .thenReturn(card1)
                .thenReturn(card2)
                .thenReturn(card3);
        
        when(gameService.getGameState(gameId)).thenReturn(gameState);
        when(gameState.getInventoryForPlayer(playerTokens.get(0))).thenReturn(inventory1);
        when(gameState.getInventoryForPlayer(playerTokens.get(1))).thenReturn(inventory2);
        when(gameState.getInventoryForPlayer(playerTokens.get(2))).thenReturn(inventory3);
        when(gameState.getPlayerInfo()).thenReturn(playerInfoMap);
        
        // Call the method
        Map<String, ActionCardDTO> result = gameRoundService.distributeFreshActionCardsByToken(gameId, playerTokens);
        
        // Verify results
        assertNotNull(result);
        assertEquals(3, result.size());
        
        assertTrue(result.containsKey(playerTokens.get(0)));
        assertTrue(result.containsKey(playerTokens.get(1)));
        assertTrue(result.containsKey(playerTokens.get(2)));
        
        assertEquals(card1, result.get(playerTokens.get(0)));
        assertEquals(card2, result.get(playerTokens.get(1)));
        assertEquals(card3, result.get(playerTokens.get(2)));
        
        // Verify interactions
        verify(inventory1).setActionCards(argThat(list -> list.contains(card1.getId())));
        verify(inventory2).setActionCards(argThat(list -> list.contains(card2.getId())));
        verify(inventory3).setActionCards(argThat(list -> list.contains(card3.getId())));
        
        verify(playerInfo1).setActionCardsLeft(1);
        verify(playerInfo2).setActionCardsLeft(1);
        verify(playerInfo3).setActionCardsLeft(1);
    }

    @Test
    public void testReplacePlayerActionCardByToken() {
        // Setup
        gameRoundService.startGame(gameId, playerTokens);
        String playerToken = playerTokens.get(0);
        
        // Create mock action cards
        ActionCardDTO oldCard = createMockActionCard("7choices", "powerup", "7 Choices", 
                "Reveal the continent of the target location.");
        ActionCardDTO newCard = createMockActionCard("badsight", "punishment", "Bad Sight", 
                "A player of your choice has their screen blurred for the first 15 seconds of the round.");
        
        // Setup mock GameState
        GameService.GameState gameState = mock(GameService.GameState.class);
        GameService.GameState.PlayerInventory inventory = mock(GameService.GameState.PlayerInventory.class);
        
        // First distribute initial cards
        when(authService.getUserByToken(anyString())).thenReturn(new User());
        when(actionCardService.drawRandomCard()).thenReturn(oldCard);
        when(gameService.getGameState(gameId)).thenReturn(gameState);
        when(gameState.getInventoryForPlayer(playerToken)).thenReturn(inventory);
        when(gameState.getPlayerInfo()).thenReturn(new HashMap<>());
        
        gameRoundService.distributeFreshActionCardsByToken(gameId, List.of(playerToken));
        
        // Reset mocks for replacement test
        reset(actionCardService);
        reset(inventory);
        
        // Setup replacement
        when(actionCardService.drawRandomCard()).thenReturn(newCard);
        when(gameService.getGameState(gameId)).thenReturn(gameState);
        when(gameState.getInventoryForPlayer(playerToken)).thenReturn(inventory);
        
        // Call the method
        ActionCardDTO result = gameRoundService.replacePlayerActionCardByToken(gameId, playerToken);
        
        // Verify result
        assertNotNull(result);
        assertEquals(newCard, result);
        
        // Verify interactions
        verify(inventory).setActionCards(argThat(list -> list.contains(newCard.getId())));
    }

    @Test
    public void testFullActionCardFlow() {
        // 1. Start a game
        gameRoundService.startGame(gameId, playerTokens);
        
        // 2. Setup mock users
        User user1 = new User();
        user1.setToken(playerTokens.get(0));
        
        User user2 = new User();
        user2.setToken(playerTokens.get(1));
        
        // 3. Create mock action cards
        ActionCardDTO card1 = createMockActionCard("7choices", "powerup", "7 Choices", 
                "Reveal the continent of the target location.");
        ActionCardDTO card2 = createMockActionCard("badsight", "punishment", "Bad Sight", 
                "A player of your choice has their screen blurred for the first 15 seconds of the round.");
        
        // 4. Setup GameState and inventories
        GameService.GameState gameState = mock(GameService.GameState.class);
        GameService.GameState.PlayerInventory inventory1 = mock(GameService.GameState.PlayerInventory.class);
        GameService.GameState.PlayerInventory inventory2 = mock(GameService.GameState.PlayerInventory.class);
        
        Map<String, GameService.GameState.PlayerInfo> playerInfoMap = new HashMap<>();
        GameService.GameState.PlayerInfo playerInfo1 = mock(GameService.GameState.PlayerInfo.class);
        GameService.GameState.PlayerInfo playerInfo2 = mock(GameService.GameState.PlayerInfo.class);
        
        playerInfoMap.put(playerTokens.get(0), playerInfo1);
        playerInfoMap.put(playerTokens.get(1), playerInfo2);
        
        // 5. Setup mocks
        when(authService.getUserByToken(playerTokens.get(0))).thenReturn(user1);
        when(authService.getUserByToken(playerTokens.get(1))).thenReturn(user2);
        
        when(actionCardService.drawRandomCard())
                .thenReturn(card1)
                .thenReturn(card2);
        
        when(gameService.getGameState(gameId)).thenReturn(gameState);
        when(gameState.getInventoryForPlayer(playerTokens.get(0))).thenReturn(inventory1);
        when(gameState.getInventoryForPlayer(playerTokens.get(1))).thenReturn(inventory2);
        when(gameState.getPlayerInfo()).thenReturn(playerInfoMap);
        
        // 6. Random assignment of cards
        Map<String, ActionCardDTO> assignedCards = gameRoundService.distributeFreshActionCardsByToken(gameId, playerTokens.subList(0, 2));
        
        // Verify assignment
        assertEquals(2, assignedCards.size());
        assertEquals(card1, assignedCards.get(playerTokens.get(0)));
        assertEquals(card2, assignedCards.get(playerTokens.get(1)));
        
        // 7. Verification of inventory updates
        verify(inventory1).setActionCards(argThat(list -> list.contains(card1.getId())));
        verify(inventory2).setActionCards(argThat(list -> list.contains(card2.getId())));
        verify(playerInfo1).setActionCardsLeft(1);
        verify(playerInfo2).setActionCardsLeft(1);
        
        // 8. Replacement of a card (consumption and getting a new one)
        ActionCardDTO replacementCard = createMockActionCard("7choices", "powerup", "7 Choices", 
                "Reveal the continent of the target location.");
        
        reset(actionCardService);
        reset(inventory1);
        
        when(actionCardService.drawRandomCard()).thenReturn(replacementCard);
        
        ActionCardDTO newCard = gameRoundService.replacePlayerActionCardByToken(gameId, playerTokens.get(0));
        
        // Verify replacement
        assertEquals(replacementCard, newCard);
        verify(inventory1).setActionCards(argThat(list -> list.contains(replacementCard.getId())));
    }
    
    private ActionCardDTO createMockActionCard(String id, String type, String title, String description) {
        ActionCardDTO card = new ActionCardDTO();
        card.setId(id);
        card.setType(type);
        card.setTitle(title);
        card.setDescription(description);
        return card;
    }
}