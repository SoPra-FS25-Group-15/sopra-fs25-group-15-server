package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@Service
@Transactional
public class AuthService {

    private final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Registration
    public User register(User newUser) {
        // Validate input
        if (newUser.getEmail() == null || newUser.getEmail().isBlank() ||
            newUser.getPassword() == null || newUser.getPassword().isBlank() ||
            newUser.getProfile().getUsername() == null || newUser.getProfile().getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or email");
        }

        // Check conflicts
        if (userRepository.findByEmail(newUser.getEmail()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already registered");
        }

        // For username conflicts, we can do:
        User existingUsername = userRepository.findByProfile_Username(newUser.getProfile().getUsername());
        if (existingUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already registered");
        }

        // Generate token, set status
        newUser.generateToken();
        newUser.setStatus(UserStatus.ONLINE);

        // Save
        newUser = userRepository.save(newUser);
        userRepository.flush();
        log.debug("Registered new user: {}", newUser);
        return newUser;
    }

    // Login
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null || !user.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        // Refresh token
        user.generateToken();
        userRepository.save(user);
        userRepository.flush();
        log.debug("User logged in: {}", user);
        return user;
    }

    // Logout
    public void logout(String token) {
        User user = getUserByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session found");
        }
        user.setToken(null);
        userRepository.save(user);
        userRepository.flush();
        log.debug("User logged out: {}", user);
    }

    // Helper method
    public User getUserByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        User user = userRepository.findByToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required");
        }
        return user;
    }

    //verify user password
    public boolean verifyPassword(User user, String password) {
        if (user == null || password == null) {
            return false;
        }
        return user.getPassword().equals(password);
    }
}

