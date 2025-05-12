package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        
        testUser = new User();
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("testUsername");

        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setProfile(userProfile);
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setToken("test-token");
    }

    @Test
    void register_validInputs_success() {
        
        User inputUser = new User();
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("newUser");

        inputUser.setEmail("new@example.com");
        inputUser.setPassword("password");
        inputUser.setProfile(userProfile);

        
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.findByProfile_Username(anyString())).thenReturn(null);
        when(userRepository.save(any())).thenReturn(inputUser);

        
        User createdUser = authService.register(inputUser);

        
        assertEquals(inputUser.getEmail(), createdUser.getEmail());
        assertEquals(UserStatus.ONLINE, createdUser.getStatus());
        assertNotNull(createdUser.getToken());

        
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).findByProfile_Username(anyString());
        verify(userRepository, times(1)).save(any());
        verify(userRepository, times(1)).flush();
    }

    @Test
    void register_duplicateEmail_throwsException() {
        
        User inputUser = new User();
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("testUsername");

        inputUser.setEmail("test@example.com");
        inputUser.setPassword("password");
        inputUser.setProfile(userProfile);

        
        when(userRepository.findByEmail(inputUser.getEmail())).thenReturn(testUser);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(inputUser)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("already registered"));

        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsException() {
        
        User inputUser = new User();
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("testUsername");

        inputUser.setEmail("new@example.com");
        inputUser.setPassword("password");
        inputUser.setProfile(userProfile);

        
        when(userRepository.findByEmail(anyString())).thenReturn(null);
        when(userRepository.findByProfile_Username(inputUser.getProfile().getUsername())).thenReturn(testUser);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(inputUser)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("already registered"));

        verify(userRepository, times(1)).findByEmail(anyString());
        verify(userRepository, times(1)).findByProfile_Username(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_invalidInput_throwsException() {
        
        User userWithoutEmail = new User();
        UserProfile userProfile1 = new UserProfile();
        userProfile1.setUsername("testUser");
        userWithoutEmail.setPassword("password");
        userWithoutEmail.setProfile(userProfile1);

        assertThrows(ResponseStatusException.class, () -> authService.register(userWithoutEmail));

        
        User userWithoutPassword = new User();
        UserProfile userProfile2 = new UserProfile();
        userProfile2.setUsername("testUser");
        userWithoutPassword.setEmail("test@example.com");
        userWithoutPassword.setProfile(userProfile2);

        assertThrows(ResponseStatusException.class, () -> authService.register(userWithoutPassword));

        
        User userWithoutUsername = new User();
        UserProfile userProfile3 = new UserProfile();
        userWithoutUsername.setEmail("test@example.com");
        userWithoutUsername.setPassword("password");
        userWithoutUsername.setProfile(userProfile3);

        assertThrows(ResponseStatusException.class, () -> authService.register(userWithoutUsername));

        
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_success() {
        
        String email = "test@example.com";
        String password = "password";

        
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        
        User loggedInUser = authService.login(email, password);

        
        assertEquals(testUser.getEmail(), loggedInUser.getEmail());
        assertNotNull(loggedInUser.getToken());

        
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(testUser);
        verify(userRepository, times(1)).flush();
    }

    @Test
    void login_invalidEmail_throwsException() {
        
        String email = "wrong@example.com";
        String password = "password";

        
        when(userRepository.findByEmail(email)).thenReturn(null);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(email, password)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid email or password"));

        
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_invalidPassword_throwsException() {
        
        String email = "test@example.com";
        String password = "wrongPassword";

        
        when(userRepository.findByEmail(email)).thenReturn(testUser);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(email, password)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid email or password"));

        
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }

    @Test
    void logout_validToken_success() {
        
        String token = "test-token";

        
        when(userRepository.findByToken(token)).thenReturn(testUser);

        
        authService.logout(token);

        
        assertNull(testUser.getToken());

        
        verify(userRepository, times(1)).findByToken(token);
        verify(userRepository, times(1)).save(testUser);
        verify(userRepository, times(1)).flush();
    }

    @Test
    void logout_invalidToken_throwsException() {
        
        String token = "invalid-token";

        
        when(userRepository.findByToken(token)).thenReturn(null);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.logout(token)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        
        verify(userRepository, times(1)).findByToken(token);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByToken_validToken_success() {
        
        String token = "test-token";

        
        when(userRepository.findByToken(token)).thenReturn(testUser);

        
        User foundUser = authService.getUserByToken(token);

        
        assertEquals(testUser, foundUser);

        
        verify(userRepository, times(1)).findByToken(token);
    }

    @Test
    void getUserByToken_bearerToken_success() {
        
        String bearerToken = "Bearer test-token";

        
        when(userRepository.findByToken("test-token")).thenReturn(testUser);

        
        User foundUser = authService.getUserByToken(bearerToken);

        
        assertEquals(testUser, foundUser);

        
        verify(userRepository, times(1)).findByToken("test-token");
    }

    @Test
    void getUserByToken_nullToken_throwsException() {
        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.getUserByToken(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("No token provided"));

        
        verify(userRepository, never()).findByToken(anyString());
    }

    @Test
    void getUserByToken_invalidToken_throwsException() {
        
        String token = "invalid-token";

        
        when(userRepository.findByToken(token)).thenReturn(null);

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.getUserByToken(token)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertTrue(exception.getReason().contains("Invalid token"));

        
        verify(userRepository, times(1)).findByToken(token);
    }

    @Test
    void getUserById_validId_success() {
        
        Long userId = 1L;

        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        
        User foundUser = authService.getUserById(userId);

        
        assertEquals(testUser, foundUser);

        
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserById_nullId_throwsException() {
        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.getUserById(null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("User ID is required"));

        
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getUserById_nonExistentId_throwsException() {
        
        Long userId = 999L;

        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.getUserById(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getReason().contains("User not found"));

        
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void verifyPassword_correctPassword_returnsTrue() {
        
        String password = "password";

        
        boolean result = authService.verifyPassword(testUser, password);

        assertTrue(result);
    }

    @Test
    void verifyPassword_incorrectPassword_returnsFalse() {
        
        String password = "wrongPassword";

        
        boolean result = authService.verifyPassword(testUser, password);

        assertFalse(result);
    }

    @Test
    void verifyPassword_nullUser_returnsFalse() {
        
        String password = "password";

        
        boolean result = authService.verifyPassword(null, password);

        assertFalse(result);
    }

    @Test
    void verifyPassword_nullPassword_returnsFalse() {
        
        boolean result = authService.verifyPassword(testUser, null);

        assertFalse(result);
    }
}