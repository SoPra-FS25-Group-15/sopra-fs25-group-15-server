package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testUser2;
    private final String VALID_TOKEN = "valid-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Set up test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setToken(VALID_TOKEN);
        
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        profile.setStatsPublic(true);
        testUser.setProfile(profile);

        // Set up secondary test user
        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setEmail("test2@example.com");
        
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("testUser2");
        testUser2.setProfile(profile2);

        // Default mock behavior
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
        when(authService.getUserByToken(VALID_TOKEN)).thenReturn(testUser);
        when(userRepository.findByEmail("test@example.com")).thenReturn(testUser);
        when(userRepository.findByProfile_Username("testUser")).thenReturn(testUser);
        when(userRepository.findByToken(VALID_TOKEN)).thenReturn(testUser);
    }

    @Test
    void getPublicProfile_success() {
        User result = userService.getPublicProfile(1L);

        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getProfile().getUsername(), result.getProfile().getUsername());
    }

    @Test
    void getPublicProfile_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            userService.getPublicProfile(99L);
        });
    }

    @Test
    void updateUser_success() {
        String newUsername = "newUsername";
        String newEmail = "new@example.com";

        User result = userService.updateUser(1L, VALID_TOKEN, newUsername, newEmail);

        assertEquals(newUsername, result.getProfile().getUsername());
        assertEquals(newEmail, result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_unauthorized() {
        // Test when someone tries to update another user's profile
        when(authService.getUserByToken(VALID_TOKEN)).thenReturn(testUser2);

        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUser(1L, VALID_TOKEN, "newUsername", "new@example.com");
        });
    }

    @Test
    void updateUser_emailConflict() {
        when(userRepository.findByEmail("taken@example.com")).thenReturn(testUser2);

        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUser(1L, VALID_TOKEN, "newUsername", "taken@example.com");
        });
    }

    @Test
    void updateUser_usernameConflict() {
        when(userRepository.findByProfile_Username("takenUsername")).thenReturn(testUser2);

        assertThrows(ResponseStatusException.class, () -> {
            userService.updateUser(1L, VALID_TOKEN, "takenUsername", "new@example.com");
        });
    }

    @Test
    void updateMyUser_success() {
        String newUsername = "newUsername";
        String newEmail = "new@example.com";
        Boolean newPrivacy = false;

        User result = userService.updateMyUser(VALID_TOKEN, newUsername, newEmail, newPrivacy);

        assertEquals(newUsername, result.getProfile().getUsername());
        assertEquals(newEmail, result.getEmail());
        assertEquals(newPrivacy, result.getProfile().isStatsPublic());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateMyUser_badRequest() {
        assertThrows(ResponseStatusException.class, () -> {
            userService.updateMyUser(VALID_TOKEN, "", "new@example.com", true);
        });
    }

    @Test
    void searchUserByEmail_success() {
        User result = userService.searchUserByEmail("test@example.com");
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    void searchUserByEmail_notFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> {
            userService.searchUserByEmail("missing@example.com");
        });
    }

    @Test
    void findByUsername_success() {
        User result = userService.findByUsername("testUser");
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    void findByUsername_notFound() {
        when(userRepository.findByProfile_Username("nonexistent")).thenReturn(null);

        assertNull(userService.findByUsername("nonexistent"));
    }

    @Test
    void getUserBySearch_withEmail() {
        User result = userService.getUserBySearch("test@example.com");
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    void getUserBySearch_withUsername() {
        when(userRepository.findByEmail("testUser")).thenReturn(null);
        when(userRepository.findByProfile_Username("testUser")).thenReturn(testUser);

        User result = userService.getUserBySearch("testUser");
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    void deleteMyAccount_success() {
        // Mock authService to verify password correctly
        doReturn(true).when(authService).verifyPassword(eq(testUser), anyString());

        userService.deleteMyAccount(VALID_TOKEN, "password");

        verify(userRepository, times(1)).delete(testUser);
        verify(userRepository, times(1)).flush();
    }

    @Test
    void deleteMyAccount_badPassword() {
        doReturn(false).when(authService).verifyPassword(eq(testUser), anyString());

        assertThrows(ResponseStatusException.class, () -> {
            userService.deleteMyAccount(VALID_TOKEN, "wrongPassword");
        });
    }

    @Test
    void getUserByToken_success() {
        User result = userService.getUserByToken("Bearer " + VALID_TOKEN);
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    void getUserByToken_invalidToken() {
        when(userRepository.findByToken("invalid-token")).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> {
            userService.getUserByToken("invalid-token");
        });
    }
}