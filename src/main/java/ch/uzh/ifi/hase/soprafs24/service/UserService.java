package ch.uzh.ifi.hase.soprafs24.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

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

    // Public profile
    public User getPublicProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
      //    (Make sure any user found with the same email/username is either null or is the current user)
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
  
}
