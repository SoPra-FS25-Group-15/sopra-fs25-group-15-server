package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private DTOMapper dtoMapper;

    private User testUser;
    private UserStatsDTO userStatsDTO;

    @BeforeEach
    void setup() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");

        // Create user profile with stats set to public
        UserProfile profile = new UserProfile();
        profile.setStatsPublic(true);
        testUser.setProfile(profile);

        // Create user stats DTO with the correct fields
        userStatsDTO = new UserStatsDTO();
        userStatsDTO.setGamesPlayed(10);
        userStatsDTO.setWins(5);
        userStatsDTO.setMmr(1500);
        userStatsDTO.setPoints(100);
    }

    @Test
    void getUserStats_whenStatsArePublic_thenReturnsStats() throws Exception {
        // given
        when(userService.getPublicProfile(1L)).thenReturn(testUser);
        when(dtoMapper.toUserStatsDTO(testUser)).thenReturn(userStatsDTO);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/1/stats")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed", is(10)))
                .andExpect(jsonPath("$.wins", is(5)))
                .andExpect(jsonPath("$.mmr", is(1500)))
                .andExpect(jsonPath("$.points", is(100)));

        verify(userService).getPublicProfile(1L);
        verify(dtoMapper).toUserStatsDTO(testUser);
    }

    @Test
    void getUserStats_whenStatsArePrivate_thenReturnsForbidden() throws Exception {
        // Set stats to private
        testUser.getProfile().setStatsPublic(false);

        // given
        when(userService.getPublicProfile(1L)).thenReturn(testUser);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/1/stats")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isForbidden());

        verify(userService).getPublicProfile(1L);
        // Verify mapper wasn't called
        Mockito.verifyNoInteractions(dtoMapper);
    }

    @Test
    void getUserStats_whenUserNotFound_thenReturnsNotFound() throws Exception {
        // given
        when(userService.getPublicProfile(99L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/99/stats")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyStats_withValidToken_thenReturnsStats() throws Exception {
        // given
        String token = "valid-token";
        when(authService.getUserByToken(token)).thenReturn(testUser);
        when(dtoMapper.toUserStatsDTO(testUser)).thenReturn(userStatsDTO);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/me/stats")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesPlayed", is(10)))
                .andExpect(jsonPath("$.wins", is(5)))
                .andExpect(jsonPath("$.mmr", is(1500)))
                .andExpect(jsonPath("$.points", is(100)));

        verify(authService).getUserByToken(token);
        verify(dtoMapper).toUserStatsDTO(testUser);
    }

    @Test
    void getMyStats_withInvalidToken_thenReturnsUnauthorized() throws Exception {
        // given
        String token = "invalid-token";
        when(authService.getUserByToken(token)).thenThrow(
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/me/stats")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyStats_withoutToken_thenReturnsBadRequest() throws Exception {
        // when/then - No Authorization header
        MockHttpServletRequestBuilder getRequest = get("/users/me/stats")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isBadRequest());
    }
}