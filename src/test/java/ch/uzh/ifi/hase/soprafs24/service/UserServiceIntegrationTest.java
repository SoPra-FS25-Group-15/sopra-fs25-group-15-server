package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

  @Qualifier("userRepository")
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @BeforeEach
  public void setup() {
    userRepository.deleteAll();
  }

  @Test
  public void updateMyUser_validInputs_success() {
    // Instead of createUser, we simulate user creation by directly persisting a user.
    User testUser = new User();
    testUser.setEmail("original@example.com");
    testUser.setPassword("password");
    testUser.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile = new UserProfile();
    profile.setUsername("originalUsername");
    // Set any other profile fields as needed.
    testUser.setProfile(profile);
    
    // Manually assign a token to simulate authentication.
    testUser.setToken("test-token");
    
    // Persist the user via the repository
    userRepository.save(testUser);
    userRepository.flush();
    
    // Now call the updateMyUser service method to update the user.
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
    // First, create two users directly via repository.
    User user1 = new User();
    user1.setEmail("first@example.com");
    user1.setPassword("password");
    user1.setStatus(UserStatus.OFFLINE);
    UserProfile profile1 = new UserProfile();
    profile1.setUsername("duplicateUsername");
    user1.setProfile(profile1);
    user1.setToken("token1");
    userRepository.save(user1);

    User user2 = new User();
    user2.setEmail("second@example.com");
    user2.setPassword("password");
    user2.setStatus(UserStatus.OFFLINE);
    UserProfile profile2 = new UserProfile();
    profile2.setUsername("uniqueUsername");
    user2.setProfile(profile2);
    user2.setToken("token2");
    userRepository.save(user2);
    userRepository.flush();
    
    // Attempt to update user2's username to a duplicate.
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
      userService.updateMyUser("token2", "duplicateUsername", "newsecond@example.com", null);
    });
    // Expect a conflict error (HTTP 409)
    assertEquals(409, exception.getStatus().value());
  }
}