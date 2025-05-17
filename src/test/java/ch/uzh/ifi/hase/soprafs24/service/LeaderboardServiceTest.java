package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
@AutoConfigureTestDatabase(replace = ANY)
class LeaderboardServiceTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private LeaderboardService leaderboardService;

    private User user1;
    private User user2;
    private User user3;
    private User user4;
    private List<User> allUsers;

    @BeforeEach
    void setUp() {
        // Reset any previous interactions with the mock
        Mockito.reset(userRepository);
        
        // Create test users with profiles and varying XP
        user1 = createUserWithXp(1L, "user1", 1000);
        user2 = createUserWithXp(2L, "user2", 2000);
        user3 = createUserWithXp(3L, "user3", 1500);
        user4 = createUserWithXp(4L, "user4", 3000);

        // Create list of all users, sorted by XP (descending)
        allUsers = new ArrayList<>();
        allUsers.add(user4); // 3000 XP
        allUsers.add(user2); // 2000 XP
        allUsers.add(user3); // 1500 XP
        allUsers.add(user1); // 1000 XP
        
        // Setup default behavior for findAll to return our pre-sorted list
        when(userRepository.findAll()).thenReturn(allUsers);
    }

    private User createUserWithXp(Long id, String username, int xp) {
        User user = new User();
        user.setId(id);

        UserProfile profile = new UserProfile();
        profile.setUsername(username);
        profile.setXp(xp);
        profile.setGamesPlayed(10);
        profile.setWins(5);

        user.setProfile(profile);
        return user;
    }

    @Test
    void getLeaderboard_ValidParameters_ReturnsCorrectPage() {
        // Arrange
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(0, 2, 3L);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(0, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(2, response.getTotalPages());
        assertEquals(2, response.getEntries().size());

        // First entry should be user4 (highest XP)
        LeaderboardEntryDTO firstEntry = response.getEntries().get(0);
        assertEquals(1, firstEntry.getRank());
        assertEquals(4L, firstEntry.getUserId());
        assertEquals("user4", firstEntry.getUsername());
        assertEquals(3000, firstEntry.getXp());
        assertFalse(firstEntry.isCurrentUser());

        // Second entry should be user2
        LeaderboardEntryDTO secondEntry = response.getEntries().get(1);
        assertEquals(2, secondEntry.getRank());
        assertEquals(2L, secondEntry.getUserId());
        assertEquals("user2", secondEntry.getUsername());
        assertEquals(2000, secondEntry.getXp());
        assertFalse(secondEntry.isCurrentUser());

        // Current user entry (user3) should be included
        LeaderboardEntryDTO currentUserEntry = response.getCurrentUserEntry();
        assertNotNull(currentUserEntry);
        assertEquals(3, currentUserEntry.getRank());
        assertEquals(3L, currentUserEntry.getUserId());
        assertEquals("user3", currentUserEntry.getUsername());
        assertEquals(1500, currentUserEntry.getXp());
        assertTrue(currentUserEntry.isCurrentUser());

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getLeaderboard_SecondPage_ReturnsCorrectEntries() {
        // Arrange
        when(userRepository.findAll()).thenReturn(allUsers);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(1, 2, 3L);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(2, response.getTotalPages());
        assertEquals(2, response.getEntries().size());

        // First entry on page 1 should be user3
        LeaderboardEntryDTO firstEntry = response.getEntries().get(0);
        assertEquals(3, firstEntry.getRank());
        assertEquals(3L, firstEntry.getUserId());
        assertEquals("user3", firstEntry.getUsername());
        assertEquals(1500, firstEntry.getXp());
        assertTrue(firstEntry.isCurrentUser());

        // Second entry on page 1 should be user1
        LeaderboardEntryDTO secondEntry = response.getEntries().get(1);
        assertEquals(4, secondEntry.getRank());
        assertEquals(1L, secondEntry.getUserId());
        assertEquals("user1", secondEntry.getUsername());
        assertEquals(1000, secondEntry.getXp());
        assertFalse(secondEntry.isCurrentUser());

        // Current user entry should match the one in the page
        LeaderboardEntryDTO currentUserEntry = response.getCurrentUserEntry();
        assertNotNull(currentUserEntry);
        assertEquals(3, currentUserEntry.getRank());
        assertEquals(3L, currentUserEntry.getUserId());
        assertEquals("user3", currentUserEntry.getUsername());
        assertTrue(currentUserEntry.isCurrentUser());
    }

    @Test
    void getLeaderboard_OutOfBoundsPage_ReturnsEmptyList() {
        // Arrange
        when(userRepository.findAll()).thenReturn(allUsers);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(5, 2, 3L);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(5, response.getPage());
        assertEquals(2, response.getPageSize());
        assertEquals(2, response.getTotalPages());
        assertTrue(response.getEntries().isEmpty());

        // Current user entry should still be included
        LeaderboardEntryDTO currentUserEntry = response.getCurrentUserEntry();
        assertNotNull(currentUserEntry);
        assertEquals(3, currentUserEntry.getRank());
        assertEquals(3L, currentUserEntry.getUserId());
        assertEquals("user3", currentUserEntry.getUsername());
        assertTrue(currentUserEntry.isCurrentUser());
    }

    @Test
    void getLeaderboard_NegativePage_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getLeaderboard(-1, 10, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Page must be greater than or equal to 0"));
    }

    @Test
    void getLeaderboard_ZeroPageSize_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getLeaderboard(0, 0, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Page size must be greater than 0"));
    }

    @Test
    void getLeaderboard_NegativePageSize_ThrowsException() {
        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getLeaderboard(0, -5, 1L);
        });
        
        assertTrue(exception.getMessage().contains("Page size must be greater than 0"));
    }

    @Test
    void getLeaderboard_NullCurrentUserId_ReturnsLeaderboardWithoutCurrentUser() {
        // Arrange
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        LeaderboardResponseDTO response = leaderboardService.getLeaderboard(0, 2, null);

        // Debug information
        System.out.println("Response entries size: " + response.getEntries().size());
        System.out.println("Current user entry: " + response.getCurrentUserEntry());
        
        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size());
        
        // Check that no entries are marked as currentUser - this should be true when currentUserId is null
        for (LeaderboardEntryDTO entry : response.getEntries()) {
            assertFalse(entry.isCurrentUser(), "No entry should be marked as currentUser when currentUserId is null");
        }
        
        // Alternative approach: if currentUserEntry is non-null, make sure it's not marked as current user
        if (response.getCurrentUserEntry() != null) {
            assertFalse(response.getCurrentUserEntry().isCurrentUser(), 
                "If currentUserEntry exists, it should not be marked as currentUser when currentUserId is null");
        }
    }

    @Test
    void getUserRank_ExistingUser_ReturnsCorrectRank() {
        // Arrange
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        int rank = leaderboardService.getUserRank(3L);

        // Assert
        assertEquals(3, rank); // user3 should be ranked 3rd
        verify(userRepository, times(1)).findById(3L);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserRank_NonExistentUser_ThrowsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getUserRank(99L);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository, times(1)).findById(99L);
    }

    @Test
    void getUserRank_UserWithoutProfile_ThrowsException() {
        // Arrange
        User userWithoutProfile = new User();
        userWithoutProfile.setId(5L);
        // No profile set
        
        when(userRepository.findById(5L)).thenReturn(Optional.of(userWithoutProfile));

        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getUserRank(5L);
        });
        
        assertTrue(exception.getMessage().contains("User has no profile"));
        verify(userRepository, times(1)).findById(5L);
    }

    @Test
    void getUserLeaderboardRange_ValidParameters_ReturnsCorrectRange() {
        // Arrange
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(3L, 1);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(3, response.getEntries().size()); // user2, user3, user1 (range 1 above and below user3)
        
        // Entries should be in correct order
        assertEquals(2, response.getEntries().get(0).getRank());
        assertEquals("user2", response.getEntries().get(0).getUsername());
        
        assertEquals(3, response.getEntries().get(1).getRank());
        assertEquals("user3", response.getEntries().get(1).getUsername());
        assertTrue(response.getEntries().get(1).isCurrentUser());
        
        assertEquals(4, response.getEntries().get(2).getRank());
        assertEquals("user1", response.getEntries().get(2).getUsername());
        
        verify(userRepository, times(1)).findById(3L);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserLeaderboardRange_UserAtTop_IncludesCorrectRange() {
        // Arrange
        when(userRepository.findById(4L)).thenReturn(Optional.of(user4)); // Top user
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(4L, 1);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size()); // Should include user4 (rank 1) and user2 (rank 2)
        
        assertEquals(1, response.getEntries().get(0).getRank());
        assertEquals("user4", response.getEntries().get(0).getUsername());
        assertTrue(response.getEntries().get(0).isCurrentUser());
        
        assertEquals(2, response.getEntries().get(1).getRank());
        assertEquals("user2", response.getEntries().get(1).getUsername());
    }

    @Test
    void getUserLeaderboardRange_UserAtBottom_IncludesCorrectRange() {
        // Arrange - make sure we're using the correct user variable
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1)); // Bottom user
        when(userRepository.findAll()).thenReturn(allUsers);

        // Act
        LeaderboardResponseDTO response = leaderboardService.getUserLeaderboardRange(1L, 1);

        // Assert
        assertEquals(4, response.getTotalPlayers());
        assertEquals(2, response.getEntries().size()); // Should include user3 (rank 3) and user1 (rank 4)
        
        assertEquals(3, response.getEntries().get(0).getRank());
        assertEquals("user3", response.getEntries().get(0).getUsername());
        
        assertEquals(4, response.getEntries().get(1).getRank());
        assertEquals("user1", response.getEntries().get(1).getUsername());
        assertTrue(response.getEntries().get(1).isCurrentUser());
    }

    @Test
    void getUserLeaderboardRange_NegativeRange_ThrowsException() {
        // Arrange
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getUserLeaderboardRange(3L, -1);
        });
        
        assertTrue(exception.getMessage().contains("Range must be greater than or equal to 0"));
    }

    @Test
    void getUserLeaderboardRange_NonExistentUser_ThrowsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResponseStatusException.class, () -> {
            leaderboardService.getUserLeaderboardRange(99L, 1);
        });
        
        assertTrue(exception.getMessage().contains("User not found"));
    }
}
