package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true",
        "jwt.secret=test-secret",
        "google.maps.api.key=TEST_KEY"
})
@Transactional
class LeaderboardServiceIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserRepository userRepository;

    private List<User> testUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Clear all existing data
        userRepository.deleteAll();
        testUsers.clear();

        // Create and save test users with different XP values
        createAndSaveUser("user1@test.com", "user1", 1000, 10, 5);
        createAndSaveUser("user2@test.com", "user2", 3000, 20, 15);
        createAndSaveUser("user3@test.com", "user3", 500, 5, 1);
        createAndSaveUser("user4@test.com", "user4", 2000, 15, 8);
        createAndSaveUser("user5@test.com", "user5", 1500, 12, 6);
    }

    private User createAndSaveUser(String email, String username, int xp, int gamesPlayed, int wins) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        
        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setXp(xp);
        profile.setGamesPlayed(gamesPlayed);
        profile.setWins(wins);
        
        user.setProfile(profile);
        
        user = userRepository.save(user);
        testUsers.add(user);
        return user;
    }

    @Test
    void getLeaderboard_ReturnsCorrectlyOrderedEntries() {
        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(0, 10, null);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(5, response.getEntries().size());
        assertEquals(1, response.getTotalPages());
        
        // Check ordering - should be sorted by XP descending
        assertEquals("user2", response.getEntries().get(0).getUsername());
        assertEquals(3000, response.getEntries().get(0).getXp());
        assertEquals(1, response.getEntries().get(0).getRank());
        
        assertEquals("user4", response.getEntries().get(1).getUsername());
        assertEquals(2000, response.getEntries().get(1).getXp());
        assertEquals(2, response.getEntries().get(1).getRank());
        
        assertEquals("user5", response.getEntries().get(2).getUsername());
        assertEquals(1500, response.getEntries().get(2).getXp());
        assertEquals(3, response.getEntries().get(2).getRank());
        
        assertEquals("user1", response.getEntries().get(3).getUsername());
        assertEquals(1000, response.getEntries().get(3).getXp());
        assertEquals(4, response.getEntries().get(3).getRank());
        
        assertEquals("user3", response.getEntries().get(4).getUsername());
        assertEquals(500, response.getEntries().get(4).getXp());
        assertEquals(5, response.getEntries().get(4).getRank());
    }

    @Test
    void getLeaderboard_WithPagination_ReturnsCorrectPage() {
        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(0, 2, null);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size());
        assertEquals(3, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(2, response.getPageSize());
        
        // First page should have users with highest XP
        assertEquals("user2", response.getEntries().get(0).getUsername());
        assertEquals("user4", response.getEntries().get(1).getUsername());
        
        // Get second page
        response = leaderboardService.getLeaderboard(1, 2, null);
        
        assertEquals(5, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size());
        assertEquals(3, response.getTotalPages());
        assertEquals(1, response.getPage());
        
        // Second page should have next highest XP
        assertEquals("user5", response.getEntries().get(0).getUsername());
        assertEquals("user1", response.getEntries().get(1).getUsername());
        
        // Get third page
        response = leaderboardService.getLeaderboard(2, 2, null);
        
        assertEquals(5, response.getTotalPlayers());
        assertEquals(1, response.getEntries().size());
        assertEquals(3, response.getTotalPages());
        assertEquals(2, response.getPage());
        
        // Third page should have lowest XP
        assertEquals("user3", response.getEntries().get(0).getUsername());
    }

    @Test
    void getLeaderboard_WithCurrentUser_IncludesCurrentUserEntry() {
        // Get the second user (user2)
        User currentUser = testUsers.get(1);
        
        // Act - request a page that doesn't contain the current user
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(2, 2, currentUser.getId());
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(1, response.getEntries().size()); // Just user3 on this page
        
        // Current user entry should still be included
        assertNotNull(response.getCurrentUserEntry());
        assertEquals(currentUser.getProfile().getUsername(), response.getCurrentUserEntry().getUsername());
        assertEquals(1, response.getCurrentUserEntry().getRank()); // user2 is rank 1
        assertTrue(response.getCurrentUserEntry().isCurrentUser());
    }

    @Test
    void getUserRank_ReturnsCorrectRank() {
        // Act
        int user1Rank = leaderboardService.getUserRank(testUsers.get(0).getId()); // user1
        int user2Rank = leaderboardService.getUserRank(testUsers.get(1).getId()); // user2 
        int user3Rank = leaderboardService.getUserRank(testUsers.get(2).getId()); // user3
        
        // Assert
        assertEquals(4, user1Rank); // user1 should be rank 4 (1000 XP)
        assertEquals(1, user2Rank); // user2 should be rank 1 (3000 XP)
        assertEquals(5, user3Rank); // user3 should be rank 5 (500 XP)
    }

    @Test
    void getUserRank_NonExistentUser_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getUserRank(9999L);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void getUserLeaderboardRange_ReturnsCorrectRange() {
        // Get the middle user (user5)
        User targetUser = testUsers.get(4); // user5 with 1500 XP (rank 3)
        
        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(targetUser.getId(), 1);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(3, response.getEntries().size()); // Should include user4, user5, user1
        
        // Check ordering and content
        assertEquals("user4", response.getEntries().get(0).getUsername());
        assertEquals(2, response.getEntries().get(0).getRank());
        
        assertEquals("user5", response.getEntries().get(1).getUsername());
        assertEquals(3, response.getEntries().get(1).getRank());
        assertTrue(response.getEntries().get(1).isCurrentUser());
        
        assertEquals("user1", response.getEntries().get(2).getUsername());
        assertEquals(4, response.getEntries().get(2).getRank());
    }

    @Test
    void getUserLeaderboardRange_UserAtTop_IncludesCorrectRange() {
        // Get the top user (user2)
        User topUser = testUsers.get(1); // user2 with 3000 XP (rank 1)
        
        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(topUser.getId(), 1);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size()); // Should include user2, user4
        
        assertEquals("user2", response.getEntries().get(0).getUsername());
        assertEquals(1, response.getEntries().get(0).getRank());
        assertTrue(response.getEntries().get(0).isCurrentUser());
        
        assertEquals("user4", response.getEntries().get(1).getUsername());
        assertEquals(2, response.getEntries().get(1).getRank());
    }

    @Test
    void getUserLeaderboardRange_UserAtBottom_IncludesCorrectRange() {
        // Get the bottom user (user3)
        User bottomUser = testUsers.get(2); // user3 with 500 XP (rank 5)
        
        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(bottomUser.getId(), 1);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size()); // Should include user1, user3
        
        assertEquals("user1", response.getEntries().get(0).getUsername());
        assertEquals(4, response.getEntries().get(0).getRank());
        
        assertEquals("user3", response.getEntries().get(1).getUsername());
        assertEquals(5, response.getEntries().get(1).getRank());
        assertTrue(response.getEntries().get(1).isCurrentUser());
    }

    @Test
    void getUserLeaderboardRange_LargeRange_IncludesAllUsers() {
        // Get a user in the middle
        User targetUser = testUsers.get(0); // user1
        
        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(targetUser.getId(), 10);
        
        // Assert
        assertEquals(5, response.getTotalPlayers());
        assertEquals(5, response.getEntries().size()); // Should include all users
        
        // Check that the target user is marked
        boolean foundTargetUser = false;
        for (int i = 0; i < response.getEntries().size(); i++) {
            if (response.getEntries().get(i).getUserId().equals(targetUser.getId())) {
                assertTrue(response.getEntries().get(i).isCurrentUser());
                foundTargetUser = true;
                break;
            }
        }
        assertTrue(foundTargetUser, "Target user should be marked as current user");
    }
}
