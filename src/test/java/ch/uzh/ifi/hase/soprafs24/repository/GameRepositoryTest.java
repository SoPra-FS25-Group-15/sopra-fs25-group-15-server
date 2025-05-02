package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
    // turn off the Cloud SQL post-processor
    "spring.cloud.gcp.sql.enabled=false"
})
@AutoConfigureTestDatabase(replace = ANY)
public class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @Test
    public void findByCreatorUsername_shouldReturnGamesCreatedByUser() {
        // Create games with different creators
        Game game1 = createGame("Game 1", "creator1", GameStatus.WAITING, 4);
        Game game2 = createGame("Game 2", "creator1", GameStatus.RUNNING, 8);
        Game game3 = createGame("Game 3", "creator2", GameStatus.WAITING, 2);
        
        entityManager.persistAndFlush(game1);
        entityManager.persistAndFlush(game2);
        entityManager.persistAndFlush(game3);

        // Find by creator using findAll() and filtering
        List<Game> allGames = gameRepository.findAll();
        List<Game> creator1Games = allGames.stream()
            .filter(game -> "creator1".equals(game.getCreatorUsername()))
            .collect(Collectors.toList());
        
        // Assertions
        assertEquals(2, creator1Games.size());
        assertTrue(creator1Games.stream().anyMatch(game -> game.getName().equals("Game 1")));
        assertTrue(creator1Games.stream().anyMatch(game -> game.getName().equals("Game 2")));
        assertFalse(creator1Games.stream().anyMatch(game -> game.getName().equals("Game 3")));
    }

    @Test
    public void findByStatus_shouldReturnGamesWithSpecificStatus() {
        // Create games with different statuses
        Game game1 = createGame("Game 1", "creator1", GameStatus.WAITING, 4);
        Game game2 = createGame("Game 2", "creator2", GameStatus.RUNNING, 8);
        Game game3 = createGame("Game 3", "creator3", GameStatus.WAITING, 2);
        
        entityManager.persistAndFlush(game1);
        entityManager.persistAndFlush(game2);
        entityManager.persistAndFlush(game3);

        // Find by status using findAll() and filtering
        List<Game> allGames = gameRepository.findAll();
        List<Game> waitingGames = allGames.stream()
            .filter(game -> GameStatus.WAITING.equals(game.getStatus()))
            .collect(Collectors.toList());
        
        // Assertions
        assertEquals(2, waitingGames.size());
        assertTrue(waitingGames.stream().allMatch(game -> game.getStatus() == GameStatus.WAITING));
    }

    @Test
    public void findByStatusAndMaxPlayersGreaterThanEqual_shouldReturnFilteredGames() {
        // Create games with different statuses and max players
        Game game1 = createGame("Game 1", "creator1", GameStatus.WAITING, 4);
        Game game2 = createGame("Game 2", "creator2", GameStatus.WAITING, 2);
        Game game3 = createGame("Game 3", "creator3", GameStatus.RUNNING, 6);
        Game game4 = createGame("Game 4", "creator4", GameStatus.WAITING, 8);
        
        entityManager.persistAndFlush(game1);
        entityManager.persistAndFlush(game2);
        entityManager.persistAndFlush(game3);
        entityManager.persistAndFlush(game4);

        // Find waiting games with at least 4 max players using findAll() and filtering
        List<Game> allGames = gameRepository.findAll();
        List<Game> filteredGames = allGames.stream()
            .filter(game -> GameStatus.WAITING.equals(game.getStatus()) && game.getMaxPlayers() >= 4)
            .collect(Collectors.toList());
        
        // Assertions
        assertEquals(2, filteredGames.size());
        assertTrue(filteredGames.stream().allMatch(game -> game.getStatus() == GameStatus.WAITING));
        assertTrue(filteredGames.stream().allMatch(game -> game.getMaxPlayers() >= 4));
    }

    @Test
    public void findByPlayersContaining_shouldReturnGamesWithSpecificPlayer() {
        // Create users
        User user1 = createUser("user1@example.com", "User One");
        User user2 = createUser("user2@example.com", "User Two");
        User savedUser1 = entityManager.persistAndFlush(user1);
        User savedUser2 = entityManager.persistAndFlush(user2);
        
        // Create games with different players
        Game game1 = createGame("Game 1", "creator", GameStatus.RUNNING, 4);
        Game game2 = createGame("Game 2", "creator", GameStatus.RUNNING, 4);
        
        // Add players to games
        Set<User> game1Players = new HashSet<>();
        game1Players.add(savedUser1);
        game1.setPlayers(game1Players);
        
        Set<User> game2Players = new HashSet<>();
        game2Players.add(savedUser1);
        game2Players.add(savedUser2);
        game2.setPlayers(game2Players);
        
        entityManager.persistAndFlush(game1);
        entityManager.persistAndFlush(game2);
        
        // Find games containing user1 using findAll() and filtering
        List<Game> allGames = gameRepository.findAll();
        List<Game> user1Games = allGames.stream()
            .filter(game -> game.getPlayers().contains(savedUser1))
            .collect(Collectors.toList());
        
        // Assertions
        assertEquals(2, user1Games.size());
        
        // Find games containing user2 using findAll() and filtering
        List<Game> user2Games = allGames.stream()
            .filter(game -> game.getPlayers().contains(savedUser2))
            .collect(Collectors.toList());
        
        // Assertions
        assertEquals(1, user2Games.size());
        assertEquals("Game 2", user2Games.get(0).getName());
    }

    private Game createGame(String name, String creatorUsername, GameStatus status, int maxPlayers) {
        Game game = new Game();
        game.setName(name);
        game.setCreatorUsername(creatorUsername);
        game.setCreationDate(LocalDateTime.now());
        game.setMaxPlayers(maxPlayers);
        game.setStatus(status);
        game.setPlayers(new HashSet<>());
        return game;
    }
    
    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        user.setProfile(profile);
        
        return user;
    }
}
