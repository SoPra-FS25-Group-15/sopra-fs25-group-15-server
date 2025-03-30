package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final UserService userService;
    private final AuthService authService;
    private final DTOMapper mapper;

    public StatsController(UserService userService, AuthService authService, DTOMapper mapper) {
        this.userService = userService;
        this.authService = authService;
        this.mapper = mapper;
    }

    /**
     * GET /api/users/{userid}/stats
     * Returns the public stats for a user. If the user's stats are set to private,
     * this endpoint will throw a 403 Forbidden.
     */
    @GetMapping("/users/{userid}/stats")
    @ResponseStatus(HttpStatus.OK)
    public UserStatsDTO getUserStats(@PathVariable Long userid) {
        User user = userService.getPublicProfile(userid);
        if (!user.getProfile().isStatsPublic()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This user's stats are private.");
        }
        return mapper.toUserStatsDTO(user);
    }

    /**
     * GET /api/users/me/stats
     * Returns the stats for the authenticated user. Requires a valid session token.
     */
    @GetMapping("/users/me/stats")
    @ResponseStatus(HttpStatus.OK)
    public UserStatsDTO getMyStats(@RequestHeader("Authorization") String token) {
        User user = authService.getUserByToken(token);
        return mapper.toUserStatsDTO(user);
    }
}
