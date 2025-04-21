package ch.uzh.ifi.hase.soprafs24.service;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

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
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
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
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
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
                
        // Create the main user that will attempt the update
        User user = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.OFFLINE);
        user.setToken("test-token");

        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        user.setProfile(profile);

        // When trying to update to a duplicate username, expect an exception
        Mockito.when(userRepository.findByToken("test-token")).thenReturn(user);
        
        // Assert that the method throws an exception
        assertThrows(ResponseStatusException.class, () -> {
            userService.updateMyUser("test-token", "duplicateUsername", "test@example.com", true);
        });
    }

    @Test
    public void deleteMyAccount_validPassword_success() {
        // Arrange
        String password = "secret";
        Mockito.when(authService.verifyPassword(testUser, password)).thenReturn(true);

        // Act
        userService.deleteMyAccount("test-token", password);

        // Assert
        Mockito.verify(userRepository, Mockito.times(1)).delete(testUser);
        Mockito.verify(userRepository, Mockito.times(1)).flush();
    }

    @Test
    public void deleteMyAccount_invalidPassword_throwsException() {
        // Arrange
        String invalidPassword = "wrongPassword";
        Mockito.when(authService.verifyPassword(testUser, invalidPassword)).thenReturn(false);

        // Act & Assert
        assertThrows(ResponseStatusException.class, () ->
                userService.deleteMyAccount("test-token", invalidPassword)
        );
    }
    
    @Test
    public void searchUserByEmail_validEmail_returnsUser() {
        // Arrange: Define test data and configure mocks
        String testEmail = "search@example.com";
        Mockito.when(userRepository.findByEmail(testEmail)).thenReturn(testUser);
        
        // Act: Call the method to be tested
        User foundUser = userService.searchUserByEmail(testEmail);
        
        // Assert: Verify the results
        assertEquals(testUser.getId(), foundUser.getId());
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        assertEquals(testUser.getProfile().getUsername(), foundUser.getProfile().getUsername());
    }
    
    @Test
    public void searchUserByEmail_invalidEmail_throwsException() {
        // Arrange: Configure mock to return null for non-existent email
        String nonExistentEmail = "nonexistent@example.com";
        Mockito.when(userRepository.findByEmail(nonExistentEmail)).thenReturn(null);
        
        // Act & Assert: Verify that exception is thrown
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.searchUserByEmail(nonExistentEmail);
        });
        
        // Verify exception details
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getReason().contains("No user found"));
    }
    
    @Test
    public void searchUserByEmail_emptyEmail_throwsException() {
        // Act & Assert: Verify that exception is thrown for blank email
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.searchUserByEmail("");
        });
        
        // Verify exception details
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Email cannot be empty"));
    }
}