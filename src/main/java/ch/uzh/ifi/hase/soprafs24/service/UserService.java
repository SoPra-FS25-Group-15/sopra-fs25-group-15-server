package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List; 
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.util.TokenUtils;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AuthService authService; // for token checks

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public User updateUser(User user) {
        return userRepository.saveAndFlush(user);
    }
    
    // Public profile
    public User getPublicProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // Update user
    public User updateUser(Long userId, String token, String newUsername, String newEmail) {
        // 1) Check if user is authenticated
        User currentUser = authService.getUserByToken(token);
        // If you only allow self-updates, check:
        if (!currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot update another user's profile");
        }

        // 2) Validate input
        if (newUsername == null || newUsername.isBlank() ||
                newEmail == null || newEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or email");
        }

        // 3) Check conflicts
        User emailCheck = userRepository.findByEmail(newEmail);
        if (emailCheck != null && !emailCheck.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already registered");
        }
        User usernameCheck = userRepository.findByProfile_Username(newUsername);
        if (usernameCheck != null && !usernameCheck.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already registered");
        }

        // 4) Perform update
        currentUser.setEmail(newEmail);
        currentUser.getProfile().setUsername(newUsername);

        userRepository.save(currentUser);
        userRepository.flush();
        log.debug("Updated user: {}", currentUser);

        return currentUser;
    }

    /**
     * Update the authenticated user's profile using their token.
     */

    public User updateMyUser(String token, String newUsername, String newEmail, Boolean newPrivacy) {
        // 1) Validate token and fetch user
        User currentUser = authService.getUserByToken(token); // throws 401 if invalid token

        // 2) Validate input
        if (newUsername == null || newUsername.isBlank() ||
                newEmail == null || newEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or email");
        }

        // 3) Check conflicts for email/username
        // (Make sure any user found with the same email/username is either null or is
        // the current user)
        User emailCheck = userRepository.findByEmail(newEmail);
        if (emailCheck != null && !emailCheck.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User usernameCheck = userRepository.findByProfile_Username(newUsername);
        if (usernameCheck != null && !usernameCheck.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already registered");
        }

        // 4) Update fields
        currentUser.setEmail(newEmail);
        currentUser.getProfile().setUsername(newUsername);
        if (newPrivacy != null) {
            currentUser.getProfile().setStatsPublic(newPrivacy);
        }

        // 5) Save changes
        userRepository.save(currentUser);
        userRepository.flush();
        log.debug("Updated user: {}", currentUser);

        return currentUser;
    }

    /*
     * Search for a user by email
     * 
     * @param email the email to search for
     * 
     * @return the found user
     * 
     * @throws ResponseStatusException if user is not found
     */
    public User searchUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with this email");
        }

        log.debug("Found user by email search: {}", user.getEmail());
        return user;

    }

    /**
     * Find a user by username
     */
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty");
        }

        User user = userRepository.findByProfile_Username(username);
        if (user == null) {
            return null; // Return null to let the caller handle the not-found case
        }

        return user;
    }

    /**
     * Find a user by search query (username or email)
     */
    public User getUserBySearch(String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query cannot be empty");
        }

        User user;
        try {
            user = userRepository.findByEmail(searchQuery);
            if (user == null) {
                user = userRepository.findByProfile_Username(searchQuery);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with this email or username");
        }

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No user found with this search query");
        }

        return user;
    }

    /**
     * delete current user account
     */
    public void deleteMyAccount(String token, String password) {
        // 1) Verify user identity and obtain user information through token
        User currentUser = authService.getUserByToken(token); // If the token is invalid, a 401 error will be thrown.

        // 2) Verify the entered password
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the password is required");
        }

        // 3) check the password is right or not
        if (!authService.verifyPassword(currentUser, password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Password verification failed, unable to delete account");
        }

        // 4) delete the user account
        userRepository.delete(currentUser);
        userRepository.flush();

        log.info("the user account has been deleted: ID={}, Email={}", currentUser.getId(), currentUser.getEmail());

    }

    /**
     * Retrieves a user based on the provided token
     * 
     * @param rawToken - the authentication token (with or without Bearer prefix)
     * @return User
     */
    public User getUserByToken(String rawToken) {
        String token = TokenUtils.extractToken(rawToken);

        User user = userRepository.findByToken(token);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        return user;
    }

    /**
 * Return the top N users sorted by descending MMR.
 */
    public List<User> getTopPlayersByMmr(int count) {
    // since we only need 10, you can directly call the repo method:
    return userRepository.findTop10ByOrderByProfileMmrDesc();
}
}
