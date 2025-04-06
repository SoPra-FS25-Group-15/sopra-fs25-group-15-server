package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final DTOMapper mapper;

    public UserController(UserService userService, DTOMapper mapper) {
        this.userService = userService;
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
    public UserUpdateResponseDTO updateMyProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody UserUpdateRequestDTO updateDTO
    ) {
        User updatedUser = userService.updateMyUser(token, updateDTO.getUsername(), updateDTO.getEmail(), updateDTO.getStatsPublic());
        return mapper.toUpdateResponse(updatedUser);
    }

    // 3) DELETE /api/users/me => delete user account
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody UserDeleteRequestDTO deleteDTO
    ) {
        userService.deleteMyAccount(token, deleteDTO.getPassword());
    }
  }


