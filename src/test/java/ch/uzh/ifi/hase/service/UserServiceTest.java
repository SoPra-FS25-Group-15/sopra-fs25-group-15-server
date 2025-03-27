package ch.uzh.ifi.hase.soprafs24.service;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private AuthService authService;  // Add this line
    
    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    
        // Create a dummy user to simulate an existing user.
        testUser = new User();
            // Use reflection to set the ID field directly
        try {
          Field idField = User.class.getDeclaredField("id");
          idField.setAccessible(true);
          idField.set(testUser, 1L);
      } catch (NoSuchFieldException | IllegalAccessException e) {
          throw new RuntimeException("Failed to set user ID via reflection", e);
      }
        testUser.setEmail("test@example.com");
        testUser.setPassword("secret");
        testUser.setStatus(UserStatus.OFFLINE);
        testUser.setToken("test-token");
        
        // Initialize the UserProfile to avoid NPE when updating.
        UserProfile profile = new UserProfile();
        profile.setUsername("testUsername");
        profile.setStatsPublic(false);
        testUser.setProfile(profile);
    
        // Configure mocks (ensure that the user returned by getUserByToken has a profile)
        Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
        Mockito.when(userRepository.findByToken("test-token")).thenReturn(testUser);
        Mockito.when(authService.getUserByToken("test-token")).thenReturn(testUser);
    }

    @Test
    public void updateMyUser_validInputs_success() {
        String newUsername = "updatedUsername";
        String newEmail = "updated@example.com";
        Boolean newPrivacy = true;
        
        // Stub conflict checks to return null (i.e. no conflict).
        Mockito.when(userRepository.findByEmail(newEmail)).thenReturn(null);
        Mockito.when(userRepository.findByProfile_Username(newUsername)).thenReturn(null);
        
        // Call the update method.
        User updatedUser = userService.updateMyUser("test-token", newUsername, newEmail, newPrivacy);
        
        // Verify that repository.save was called.
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());
        
        // Assertions for updated values.
        assertEquals(newEmail, updatedUser.getEmail());
        assertEquals(newUsername, updatedUser.getProfile().getUsername());
        assertEquals(newPrivacy, updatedUser.getProfile().isStatsPublic());
    }
    
    @Test
    public void updateMyUser_duplicateUsername_throwsException() {
        // Create a conflict user with complete profile information.
        User conflictUser = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(conflictUser, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        conflictUser.setEmail("other@example.com");
        conflictUser.setPassword("password");
        conflictUser.setStatus(UserStatus.OFFLINE);
        conflictUser.setToken("conflict-token");

        UserProfile conflictProfile = new UserProfile();
        conflictProfile.setUsername("duplicateUsername");
        conflictProfile.setStatsPublic(false);
        conflictUser.setProfile(conflictProfile);
        
        // Simulate that a user with the duplicate username already exists.
        Mockito.when(userRepository.findByProfile_Username("duplicateUsername"))
                .thenReturn(conflictUser);
        
        // Also, let the email lookup return null (no conflict by email).
        Mockito.when(userRepository.findByEmail("updated@example.com")).thenReturn(null);
        
        // Attempt to update testUser with the duplicate username.
        assertThrows(ResponseStatusException.class, () ->
            userService.updateMyUser("test-token", "duplicateUsername", "updated@example.com", null)
        );
    }
}