package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ActionCardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActionCardService actionCardService;

    @Autowired
    private UserRepository userRepository;

    private static final Long TEST_GAME_ID = 1L;
    private static final String TEST_TOKEN = "test-token";

    @BeforeEach
    public void setup() {
        // Create a test user with all required fields
        User testUser = userRepository.findByToken(TEST_TOKEN);
        if (testUser == null) {
            testUser = new User();
            testUser.setToken(TEST_TOKEN);
            testUser.setEmail("test@example.com");
            testUser.setPassword("password");
            testUser.setStatus(UserStatus.ONLINE);
            // Build minimal profile
            UserProfile profile = new UserProfile();
            profile.setUsername("testUser");
            profile.setMmr(0);
            profile.setPoints(0);
            profile.setGamesPlayed(0);
            profile.setWins(0);
            profile.setStatsPublic(true);
            testUser.setProfile(profile);
            userRepository.save(testUser);
        }
    }

    @Test
    public void getRandomCard_validRequest_returnCard() throws Exception {
        // Mock the service to return a specific card
        ActionCardDTO mockCard = new ActionCardDTO();
        mockCard.setId("7choices");
        mockCard.setType("powerup");
        mockCard.setTitle("7 Choices");
        mockCard.setDescription("Reveal the continent of the target location.");
        
        when(actionCardService.drawRandomCard()).thenReturn(mockCard);

        // Perform the request
        MockHttpServletRequestBuilder getRequest = get("/game/" + TEST_GAME_ID + "/actionCards/random")
                .header("Authorization", TEST_TOKEN)
                .contentType(MediaType.APPLICATION_JSON);

        // Verify the response
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("7choices")))
                .andExpect(jsonPath("$.type", is("powerup")))
                .andExpect(jsonPath("$.title", is("7 Choices")))
                .andExpect(jsonPath("$.description", is("Reveal the continent of the target location.")));
    }
    
    @Test
    public void getRandomCard_invalidToken_unauthorized() throws Exception {
        // Perform the request with an invalid token
        MockHttpServletRequestBuilder getRequest = get("/game/" + TEST_GAME_ID + "/actionCards/random")
                .header("Authorization", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON);

        // Should return unauthorized
        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }
}
