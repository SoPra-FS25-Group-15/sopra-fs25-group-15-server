package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.TestPropertySource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
@WebMvcTest(StatsController.class)
public class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;
    
    @MockBean
    private DTOMapper mapper;

    @Test
    public void getUserStats_PublicStats_ReturnsStats() throws Exception {
        // given
        User user = new User();
        // Set ID via reflection because there is no public setId method
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ONLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        profile.setMmr(1500);
        profile.setPoints(1500);
        profile.setWins(5);
        profile.setGamesPlayed(7); // Added this since there's no setter for losses directly
        profile.setStatsPublic(true); // Public stats
        profile.setAchievements(Arrays.asList("First Win"));
        user.setProfile(profile);

        // Expected DTO
        UserStatsDTO statsDTO = new UserStatsDTO();
        statsDTO.setGamesPlayed(7);
        statsDTO.setWins(5);
        statsDTO.setMmr(1500);
        statsDTO.setPoints(1500);

        given(userService.getPublicProfile(eq(1L))).willReturn(user);
        given(mapper.toUserStatsDTO(user)).willReturn(statsDTO);

        // when/then
        mockMvc.perform(get("/users/1/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed").value(7))
                .andExpect(jsonPath("$.wins").value(5))
                .andExpect(jsonPath("$.mmr").value(1500))
                .andExpect(jsonPath("$.points").value(1500));
    }

    @Test
    public void getUserStats_PrivateStats_ReturnsForbidden() throws Exception {
        // given
        User user = new User();
        // Set ID via reflection
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ONLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        profile.setMmr(1500);
        profile.setStatsPublic(false); // Private stats
        user.setProfile(profile);

        given(userService.getPublicProfile(eq(1L))).willReturn(user);

        // when/then
        mockMvc.perform(get("/users/1/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getMyStats_ValidToken_ReturnsStats() throws Exception {
        // given
        final String token = "Bearer test-token";
        User user = new User();
        // Set ID via reflection
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ONLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        profile.setMmr(1500);
        profile.setPoints(1500);
        profile.setWins(5);
        profile.setGamesPlayed(7);
        profile.setStatsPublic(false); // Doesn't matter for personal stats
        user.setProfile(profile);

        // Expected DTO
        UserStatsDTO statsDTO = new UserStatsDTO();
        statsDTO.setGamesPlayed(7);
        statsDTO.setWins(5);
        statsDTO.setMmr(1500);
        statsDTO.setPoints(1500);

        given(authService.getUserByToken(eq(token))).willReturn(user);
        given(mapper.toUserStatsDTO(user)).willReturn(statsDTO);

        // when/then
        mockMvc.perform(get("/users/me/stats")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed").value(7))
                .andExpect(jsonPath("$.wins").value(5))
                .andExpect(jsonPath("$.mmr").value(1500))
                .andExpect(jsonPath("$.points").value(1500));
    }
}
