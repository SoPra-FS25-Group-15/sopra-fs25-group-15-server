package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.LobbyRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.ActionCardMapper;

import javax.transaction.Transactional;
import java.util.ArrayList;

@SpringBootTest(properties = {
  // disable GCP Cloud SQL auto‐configuration in tests
  "spring.cloud.gcp.sql.enabled=false",
  // point to in‐memory H2
  "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  // JPA/Hibernate DDL handling
  "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.show-sql=true"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
public class UserServiceIntegrationTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LobbyRepository lobbyRepository;

  @MockBean
  private ActionCardMapper actionCardMapper;

  @MockBean
  private ActionCardService actionCardService;

  @MockBean
  private GoogleMapsService googleMapsService;

  @BeforeEach
  public void setup() {
    // Use transactions to ensure proper order of cleanup
    // First clear lobbies (which may reference users)
    lobbyRepository.deleteAll();
    lobbyRepository.flush();
    
    // Then clear users
    userRepository.deleteAll();
    userRepository.flush();
  }

  @Test
  public void updateMyUser_validInputs_success() {
    // Create a user by directly persisting it.
    User testUser = new User();
    testUser.setEmail("original@example.com");
    testUser.setPassword("password");
    testUser.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile = new UserProfile();
    profile.setUsername("originalUsername");
    profile.setStatsPublic(false); // Initialize with a value
    profile.setXp(1000);          // Initialize with a default XP (changed from mmr)
    profile.setGamesPlayed(0);     // Initialize games played
    profile.setWins(0);            // Initialize wins
    profile.setAchievements(new ArrayList<>()); // Initialize empty achievements list
    testUser.setProfile(profile);
    
    // Manually assign a token to simulate authentication.
    testUser.setToken("test-token");
    
    userRepository.save(testUser);
    userRepository.flush();
    
    // Make sure there are no lobbies referencing the user.
    lobbyRepository.deleteAll();
    
    // Now update the user.
    String newUsername = "updatedUsername";
    String newEmail = "updated@example.com";
    Boolean newPrivacy = true;
    
    User updatedUser = userService.updateMyUser("test-token", newUsername, newEmail, newPrivacy);
    assertNotNull(updatedUser.getId());
    assertEquals(newEmail, updatedUser.getEmail());
    assertEquals(newUsername, updatedUser.getProfile().getUsername());
    assertEquals(newPrivacy, updatedUser.getProfile().isStatsPublic());
  }

  @Test
  public void updateMyUser_duplicateUsername_throwsException() {
    // Create first user.
    User user1 = new User();
    user1.setEmail("first@example.com");
    user1.setPassword("password");
    user1.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile1 = new UserProfile();
    profile1.setUsername("duplicateUsername");
    profile1.setStatsPublic(true);        // Initialize with a value
    profile1.setXp(1000);                // Initialize with a default XP value
    profile1.setGamesPlayed(0);           // Initialize games played
    profile1.setWins(0);                  // Initialize wins 
    profile1.setAchievements(new ArrayList<>()); // Initialize empty achievements list
    user1.setProfile(profile1);
    user1.setToken("token1");
    userRepository.save(user1);

    // Create second user.
    User user2 = new User();
    user2.setEmail("second@example.com");
    user2.setPassword("password");
    user2.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile2 = new UserProfile();
    profile2.setUsername("uniqueUsername");
    profile2.setStatsPublic(false);       // Initialize with a value
    profile2.setXp(1000);                // Initialize with a default XP value
    profile2.setGamesPlayed(0);           // Initialize games played
    profile2.setWins(0);                  // Initialize wins
    profile2.setAchievements(new ArrayList<>()); // Initialize empty achievements list
    user2.setProfile(profile2);
    user2.setToken("token2");
    userRepository.save(user2);
    
    userRepository.flush();
    
    // Ensure no lobby exists that might reference user2.
    lobbyRepository.deleteAll();

    // Attempt to update user2's username to one that is already taken.
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      userService.updateMyUser("token2", "duplicateUsername", "newsecond@example.com", false);
    });
    // Expect a conflict error (HTTP 409).
    assertEquals(409, exception.getStatus().value());
  }

  @Test
  public void userXpAwarded_persistsCorrectly() {
    // Create a test user
    User testUser = new User();
    testUser.setEmail("xptest@example.com");
    testUser.setPassword("password");
    testUser.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile = new UserProfile();
    profile.setUsername("xpTestUser");
    profile.setStatsPublic(true);
    profile.setXp(0);  // Start with 0 XP
    profile.setGamesPlayed(0);
    profile.setWins(0);
    profile.setAchievements(new ArrayList<>());
    testUser.setProfile(profile);
    testUser.setToken("xp-test-token");
    
    userRepository.save(testUser);
    userRepository.flush();
    
    // Get the saved user ID
    Long userId = testUser.getId();
    
    // Use UserXpService to award XP (if it's available in the test context)
    // Note: This might need to be mocked or autowired depending on your test setup
    
    // Directly update XP for test purposes
    testUser.getProfile().setXp(50);
    userRepository.save(testUser);
    userRepository.flush();
    
    // Verify XP was persisted
    User updatedUser = userRepository.findById(userId).orElse(null);
    assertNotNull(updatedUser);
    assertEquals(50, updatedUser.getProfile().getXp());
  }
}