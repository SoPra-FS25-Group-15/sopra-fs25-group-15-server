package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserSearchRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserDeleteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final DTOMapper mapper;

    public UserController(UserService userService, AuthService authService, DTOMapper mapper) {
        this.userService = userService;
        this.authService = authService;
        this.mapper = mapper;
    }

    // 1) GET /api/users/{userid} => public profile
    @GetMapping("/{userid}")
    @ResponseStatus(HttpStatus.OK)
    public UserPublicDTO getPublicProfile(@PathVariable Long userid) {
        User user = userService.getPublicProfile(userid);
        return mapper.toUserPublicDTO(user);
    }
  
    // 2) PUT /api/users/me => update user
    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserMeDTO updateMyProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserUpdateRequestDTO updateDTO
    ) {
        User updatedUser = userService.updateMyUser(token, updateDTO.getUsername(), updateDTO.getEmail(), updateDTO.getStatsPublic());
        return mapper.toUserMeDTO(updatedUser);
    }

  
    // 3) POST /api/users/search => search for users by email
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public UserSearchResponseDTO searchUsers(
            @RequestHeader("Authorization") String token,
            @RequestBody UserSearchRequestDTO searchDTO
    ) {
        // First verify authentication
        // This will throw an exception if the token is invalid
        authService.getUserByToken(token);
        
        // Then search for the user by email
        User foundUser = userService.searchUserByEmail(searchDTO.getEmail());
        return mapper.toUserSearchResponseDTO(foundUser);
    }
  
    // 4) DELETE /api/users/me => delete user account
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody UserDeleteRequestDTO deleteDTO
    ) {
        userService.deleteMyAccount(token, deleteDTO.getPassword());
    }
  }


