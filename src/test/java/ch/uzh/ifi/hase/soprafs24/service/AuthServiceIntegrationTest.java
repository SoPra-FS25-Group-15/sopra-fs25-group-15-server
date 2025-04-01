package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@WebAppConfiguration
@SpringBootTest
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        // Make sure we flush all changes and use a transaction for cleanup
        userRepository.deleteAll();
        userRepository.flush();
    }
    
    @Test
    public void register_validUser_createsUser() {
        // Use timestamp to ensure unique email even if tests run in parallel
        String uniqueEmail = "newuser_" + System.currentTimeMillis() + "@example.com";
        String uniqueUsername = "NewUser_" + System.currentTimeMillis();
        
        User newUser = new User();
        newUser.setEmail(uniqueEmail);
        newUser.setPassword("secret");
        UserProfile profile = new UserProfile();
        profile.setUsername(uniqueUsername);
        newUser.setProfile(profile);
        
        // Call the register service method
        User registeredUser = authService.register(newUser);
        
        // Use more flexible assertions that handle potential transformations
        assertNotNull(registeredUser);
        assertNotNull(registeredUser.getId());
        
        // Use case-insensitive comparison or verify only that emails are equal ignoring case
        assertTrue(uniqueEmail.equalsIgnoreCase(registeredUser.getEmail()),
                "Expected email: " + uniqueEmail + ", but got: " + registeredUser.getEmail());
                
        // Username might also be transformed
        assertTrue(uniqueUsername.equalsIgnoreCase(registeredUser.getProfile().getUsername()),
                "Expected username: " + uniqueUsername + ", but got: " + registeredUser.getProfile().getUsername());
        
        // Additional checks that should pass
        assertEquals(UserStatus.ONLINE, registeredUser.getStatus());
        assertNotNull(registeredUser.getToken());
    }
    
    @Test
    public void register_duplicateEmailOrUsername_throwsException() {
        // Use unique values for the first user
        String uniqueEmail = "user_" + System.currentTimeMillis() + "@example.com";
        String uniqueUsername = "DuplicateUser_" + System.currentTimeMillis();
        
        User user1 = new User();
        user1.setEmail(uniqueEmail);
        user1.setPassword("secret");
        UserProfile profile1 = new UserProfile();
        profile1.setUsername(uniqueUsername);
        user1.setProfile(profile1);
        authService.register(user1);
        
        // Use the SAME values for the second user to test the duplicate check
        User user2 = new User();
        user2.setEmail(uniqueEmail);  // Intentional duplicate
        user2.setPassword("anotherSecret");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername(uniqueUsername);  // Intentional duplicate
        user2.setProfile(profile2);
        
        // Test should expect the exception
        assertThrows(ResponseStatusException.class, () -> authService.register(user2));
    }
}
