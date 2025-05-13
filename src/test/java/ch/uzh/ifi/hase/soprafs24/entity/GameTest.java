package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameTest {

    private Game game;
    private User player1;
    private User player2;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        game = new Game();

        player1 = new User();
        player1.setId(1L);
        player1.setUsername("player1");

        player2 = new User();
        player2.setId(2L);
        player2.setUsername("player2");

        testDateTime = LocalDateTime.now();
    }

    @Test
    void testConstructor_Default() {
        Game newGame = new Game();
        assertNotNull(newGame);
        assertNull(newGame.getId());
        assertNull(newGame.getName());
        assertNull(newGame.getCreatorUsername());
        assertNull(newGame.getCreationDate());
        assertNull(newGame.getMaxPlayers());
        assertNull(newGame.getStatus());
        assertNotNull(newGame.getPlayers());
        assertTrue(newGame.getPlayers().isEmpty());
        assertEquals(0, newGame.getCurrentRound());
        assertNull(newGame.getCurrentRoundWinnerId());
        assertNull(newGame.getWinner());
    }

    @Test
    void testId() {
        Long id = 1L;
        game.setId(id);
        assertEquals(id, game.getId());
    }

    @Test
    void testName() {
        String name = "Test Game";
        game.setName(name);
        assertEquals(name, game.getName());
    }

    @Test
    void testCreatorUsername() {
        String creatorUsername = "creator";
        game.setCreatorUsername(creatorUsername);
        assertEquals(creatorUsername, game.getCreatorUsername());
    }

    @Test
    void testCreationDate() {
        game.setCreationDate(testDateTime);
        assertEquals(testDateTime, game.getCreationDate());
    }

    @Test
    void testMaxPlayers() {
        Integer maxPlayers = 4;
        game.setMaxPlayers(maxPlayers);
        assertEquals(maxPlayers, game.getMaxPlayers());
    }

    @Test
    void testPlayers() {
        Set<User> players = new HashSet<>();
        players.add(player1);
        players.add(player2);

        game.setPlayers(players);
        assertEquals(players, game.getPlayers());
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player1));
        assertTrue(game.getPlayers().contains(player2));
    }

    @Test
    void testAddAndRemovePlayers() {
        // Add players one by one
        game.getPlayers().add(player1);
        assertEquals(1, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player1));

        game.getPlayers().add(player2);
        assertEquals(2, game.getPlayers().size());
        assertTrue(game.getPlayers().contains(player2));

        // Remove a player
        game.getPlayers().remove(player1);
        assertEquals(1, game.getPlayers().size());
        assertFalse(game.getPlayers().contains(player1));
        assertTrue(game.getPlayers().contains(player2));
    }

    @Test
    void testCurrentRound() {
        Integer currentRound = 3;
        game.setCurrentRound(currentRound);
        assertEquals(currentRound, game.getCurrentRound());
    }

    @Test
    void testCurrentRoundWinnerId() {
        Long winnerId = 5L;
        game.setCurrentRoundWinnerId(winnerId);
        assertEquals(winnerId, game.getCurrentRoundWinnerId());
    }

    @Test
    void testWinner() {
        game.setWinner(player1);
        assertEquals(player1, game.getWinner());
    }

    @Test
    void testFullyPopulatedGame() {
        Long id = 1L;
        String name = "Full Game";
        String creatorUsername = "gameCreator";
        Integer maxPlayers = 4;
        GameStatus status = GameStatus.WAITING;
        Set<User> players = new HashSet<>();
        players.add(player1);
        players.add(player2);
        Integer currentRound = 5;
        Long currentRoundWinnerId = player1.getId();

        game.setId(id);
        game.setName(name);
        game.setCreatorUsername(creatorUsername);
        game.setCreationDate(testDateTime);
        game.setMaxPlayers(maxPlayers);
        game.setStatus(status);
        game.setPlayers(players);
        game.setCurrentRound(currentRound);
        game.setCurrentRoundWinnerId(currentRoundWinnerId);
        game.setWinner(player1);

        assertEquals(id, game.getId());
        assertEquals(name, game.getName());
        assertEquals(creatorUsername, game.getCreatorUsername());
        assertEquals(testDateTime, game.getCreationDate());
        assertEquals(maxPlayers, game.getMaxPlayers());
        assertEquals(status, game.getStatus());
        assertEquals(players, game.getPlayers());
        assertEquals(currentRound, game.getCurrentRound());
        assertEquals(currentRoundWinnerId, game.getCurrentRoundWinnerId());
        assertEquals(player1, game.getWinner());
    }

    @Test
    void testNullValues() {
        game.setId(null);
        game.setName(null);
        game.setCreatorUsername(null);
        game.setCreationDate(null);
        game.setMaxPlayers(null);
        game.setStatus(null);
        game.setPlayers(null);
        game.setCurrentRound(null);
        game.setCurrentRoundWinnerId(null);
        game.setWinner(null);

        assertNull(game.getId());
        assertNull(game.getName());
        assertNull(game.getCreatorUsername());
        assertNull(game.getCreationDate());
        assertNull(game.getMaxPlayers());
        assertNull(game.getStatus());
        assertNull(game.getPlayers());
        assertNull(game.getCurrentRound());
        assertNull(game.getCurrentRoundWinnerId());
        assertNull(game.getWinner());
    }

    @Test
    void testEmptyPlayersSet() {
        game.setPlayers(new HashSet<>());
        assertNotNull(game.getPlayers());
        assertTrue(game.getPlayers().isEmpty());
    }

    @Test
    void testCurrentRoundDefault() {
        // New game should have currentRound set to 0
        Game newGame = new Game();
        assertEquals(0, newGame.getCurrentRound());
    }
}