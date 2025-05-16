package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardEntryDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LeaderboardResponseDTO;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LeaderboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeaderboardService leaderboardService;

    @MockBean
    private AuthService authService;

    private static final String VALID_TOKEN = "valid-token";
    private static final String BEARER_VALID_TOKEN = "Bearer valid-token";
    private static final String INVALID_TOKEN = "invalid-token";

    private User validUser;
    private LeaderboardResponseDTO mockLeaderboardResponse;
    private List<LeaderboardEntryDTO> mockEntries;

    @BeforeEach
    void setUp() {
        // Set up valid user
        validUser = new User();
        validUser.setId(1L);
        
        // Important: Configure auth service to accept both token variants
        when(authService.getUserByToken(VALID_TOKEN)).thenReturn(validUser);
        when(authService.getUserByToken("Bearer " + VALID_TOKEN)).thenReturn(validUser);
        when(authService.getUserByToken(INVALID_TOKEN))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));

        // Set up mock leaderboard data
        mockEntries = new ArrayList<>();
        LeaderboardEntryDTO entry1 = new LeaderboardEntryDTO();
        entry1.setRank(1);
        entry1.setUserId(2L);
        entry1.setUsername("topPlayer");
        entry1.setXp(5000);
        entry1.setGamesPlayed(50);
        entry1.setWins(25);
        
        LeaderboardEntryDTO entry2 = new LeaderboardEntryDTO();
        entry2.setRank(2);
        entry2.setUserId(3L);
        entry2.setUsername("secondPlayer");
        entry2.setXp(4000);
        entry2.setGamesPlayed(40);
        entry2.setWins(18);
        
        LeaderboardEntryDTO currentUserEntry = new LeaderboardEntryDTO();
        currentUserEntry.setRank(10);
        currentUserEntry.setUserId(1L);
        currentUserEntry.setUsername("currentUser");
        currentUserEntry.setXp(1000);
        currentUserEntry.setGamesPlayed(20);
        currentUserEntry.setWins(8);
        currentUserEntry.setCurrentUser(true);
        
        mockEntries.add(entry1);
        mockEntries.add(entry2);
        
        mockLeaderboardResponse = new LeaderboardResponseDTO();
        mockLeaderboardResponse.setEntries(mockEntries);
        mockLeaderboardResponse.setTotalPlayers(100);
        mockLeaderboardResponse.setPage(0);
        mockLeaderboardResponse.setPageSize(2);
        mockLeaderboardResponse.setTotalPages(50);
        mockLeaderboardResponse.setCurrentUserEntry(currentUserEntry);
    }

    @Test
    void getLeaderboard_ValidToken_ReturnsLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getLeaderboard(eq(0), eq(2), eq(1L))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard")
                .header("Authorization", BEARER_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].username").value("topPlayer"))
                .andExpect(jsonPath("$.entries[0].xp").value(5000))
                .andExpect(jsonPath("$.entries[1].rank").value(2))
                .andExpect(jsonPath("$.entries[1].username").value("secondPlayer"))
                .andExpect(jsonPath("$.totalPlayers").value(100))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalPages").value(50))
                .andExpect(jsonPath("$.currentUserEntry").exists())
                .andExpect(jsonPath("$.currentUserEntry.rank").value(10))
                .andExpect(jsonPath("$.currentUserEntry.username").value("currentUser"));
    }

    @Test
    void getLeaderboard_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/leaderboard")
                .header("Authorization", INVALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLeaderboard_WithCustomParams_ReturnsCorrectLeaderboard() throws Exception {
        // Arrange
        when(leaderboardService.getLeaderboard(eq(2), eq(5), eq(1L))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard")
                .header("Authorization", BEARER_VALID_TOKEN)
                .param("page", "2")
                .param("pageSize", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.entries.length()").value(2));
    }

    @Test
    void getUserRank_ValidToken_ReturnsRank() throws Exception {
        // Arrange
        when(leaderboardService.getUserRank(eq(5L))).thenReturn(15);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/5/rank")
                .header("Authorization", BEARER_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").value(15));
    }

    @Test
    void getUserRank_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/5/rank")
                .header("Authorization", INVALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserRank_NonExistentUser_ReturnsNotFound() throws Exception {
        // Arrange
        when(leaderboardService.getUserRank(eq(999L)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/999/rank")
                .header("Authorization", BEARER_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserLeaderboardRange_ValidToken_ReturnsLeaderboardRange() throws Exception {
        // Arrange
        when(leaderboardService.getUserLeaderboardRange(eq(1L), eq(2))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/1/range")
                .header("Authorization", BEARER_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.currentUserEntry").exists())
                .andExpect(jsonPath("$.currentUserEntry.username").exists());
    }

    @Test
    void getUserLeaderboardRange_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/1/range")
                .header("Authorization", INVALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserLeaderboardRange_WithCustomRange_ReturnsCorrectRange() throws Exception {
        // Arrange
        when(leaderboardService.getUserLeaderboardRange(eq(1L), eq(10))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/users/1/range")
                .header("Authorization", BEARER_VALID_TOKEN)
                .param("range", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.entries.length()").value(2));
    }

    @Test
    void getTopPlayers_ValidToken_ReturnsTopPlayers() throws Exception {
        // Arrange
        when(leaderboardService.getLeaderboard(eq(0), eq(2), eq(1L))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/top")
                .header("Authorization", BEARER_VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].username").exists());
    }

    @Test
    void getTopPlayers_InvalidToken_ReturnsUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/leaderboard/top")
                .header("Authorization", INVALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTopPlayers_WithCustomCount_ReturnsCorrectNumberOfPlayers() throws Exception {
        // Arrange
        when(leaderboardService.getLeaderboard(eq(0), eq(5), eq(1L))).thenReturn(mockLeaderboardResponse);

        // Act & Assert
        mockMvc.perform(get("/leaderboard/top")
                .header("Authorization", BEARER_VALID_TOKEN)
                .param("count", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.entries.length()").value(2));
    }
}
