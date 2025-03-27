package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        userRepository.deleteAll();
    }

    @Test
    public void register_validUser_createsUser() {
        // given: a valid new user with email, password, and username in the UserProfile
        User newUser = new User();
        newUser.setEmail("newuser@example.com");
        newUser.setPassword("secret");
        UserProfile profile = new UserProfile();
        profile.setUsername("NewUser");
        newUser.setProfile(profile);

        // when: registering the new user
        User registeredUser = authService.register(newUser);

        // then
        assertNotNull(registeredUser.getId());
        assertNotNull(registeredUser.getToken());
        assertEquals("newuser@example.com", registeredUser.getEmail());
        assertEquals("NewUser", registeredUser.getProfile().getUsername());
        assertEquals(UserStatus.OFFLINE, registeredUser.getStatus());
    }

    @Test
    public void register_duplicateEmailOrUsername_throwsException() {
        // given: a user is already registered
        User user1 = new User();
        user1.setEmail("user@example.com");
        user1.setPassword("secret");
        UserProfile profile1 = new UserProfile();
        profile1.setUsername("DuplicateUser");
        user1.setProfile(profile1);
        authService.register(user1);

        // when: attempting to register a second user with the same email and username
        User user2 = new User();
        user2.setEmail("user@example.com"); // duplicate email
        user2.setPassword("anotherSecret");
        UserProfile profile2 = new UserProfile();
        profile2.setUsername("DuplicateUser"); // duplicate username
        user2.setProfile(profile2);

        // then: expect a ResponseStatusException (HTTP 409 Conflict)
        assertThrows(ResponseStatusException.class, () -> authService.register(user2));
    }
}
