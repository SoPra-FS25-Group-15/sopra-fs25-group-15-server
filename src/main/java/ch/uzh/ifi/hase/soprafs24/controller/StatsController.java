package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List; 
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameRecordDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.StatsService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@RestController
@RequestMapping("/users")
public class StatsController {

    private final UserService userService;
    private final AuthService authService;
    private final DTOMapper mapper;
    private final StatsService statsService;

    public StatsController(UserService userService, AuthService authService, DTOMapper mapper, StatsService statsService) {
        this.userService = userService;
        this.authService = authService;
        this.mapper = mapper;
        this.statsService  = statsService;

    }

    /**
     * GET /api/users/{userid}/stats
     * Returns the public stats for a user. If the user's stats are set to private,
     * this endpoint will throw a 403 Forbidden.
     */
    @GetMapping("/{userid}/stats")
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
    @GetMapping("/me/stats")
    @ResponseStatus(HttpStatus.OK)
    public UserStatsDTO getMyStats(@RequestHeader("Authorization") String token) {
        User user = authService.getUserByToken(token);
        return mapper.toUserStatsDTO(user);
    }

    @GetMapping("/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    public LeaderboardDTO getLeaderboard() {
        List<User> top10 = userService.getTopPlayersByMmr(10);
        List<LeaderboardEntryDTO> entries = top10.stream()
            .map(mapper::toLeaderboardEntryDTO)
            .collect(Collectors.toList());
        return new LeaderboardDTO(entries);
    }

    @GetMapping("/{userid}/stats/games")
    @ResponseStatus(HttpStatus.OK)
    public List<GameRecordDTO> getGameRecords(
            @PathVariable Long userid,
            @RequestHeader("Authorization") String token) {

        var current = authService.getUserByToken(token);
        if (!current.getId().equals(userid)) {
            // only allow if public
            var user = userService.getPublicProfile(userid);
            if (!user.getProfile().isStatsPublic()) {
                throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This user’s game history is private."
                );
            }
        }
        return statsService.getGameRecords(userid);
    }
}
