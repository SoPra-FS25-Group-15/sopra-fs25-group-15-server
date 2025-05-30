package ch.uzh.ifi.hase.soprafs24.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserDeleteRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserSearchRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserSearchResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private DTOMapper mapper;

    // Test for GET /api/users/{userid}
    @Test
    public void getPublicProfile_validId_returnsUserPublicDTO() throws Exception {
        // given
        User user = new User();
        // Set ID via reflection because there is no public setId method
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("firstname@lastname.com");
        user.setStatus(UserStatus.OFFLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("firstnameLastname");
        profile.setXp(1500); // Changed from setMmr to setXp
        profile.setPoints(1500); // Add points to match the DTO
        profile.setAchievements(Arrays.asList("First Win"));
        user.setProfile(profile);

        // Expected DTO; note that the mapper converts the entity to a DTO.
        UserPublicDTO publicDTO = new UserPublicDTO();
        publicDTO.setUserid(1L);
        publicDTO.setUsername(profile.getUsername());
        publicDTO.setXp(profile.getXp()); // Changed from setMmr/getMmr to setXp/getXp
        publicDTO.setPoints(profile.getPoints()); // Set points to be returned
        publicDTO.setEmail(user.getEmail());
        // No need to set achievements as they are @JsonIgnore'd
        publicDTO.setAchievements(profile.getAchievements());

        given(userService.getPublicProfile(eq(1L))).willReturn(user);
        given(mapper.toUserPublicDTO(user)).willReturn(publicDTO);

        // when/then
        mockMvc.perform(get("/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid", is(1)))
                .andExpect(jsonPath("$.username", is(profile.getUsername())))
                .andExpect(jsonPath("$.xp", is(profile.getXp()))) // Changed from mmr to xp
                .andExpect(jsonPath("$.points", is(profile.getPoints())))
                .andExpect(jsonPath("$.email", is(user.getEmail())));
        // Remove the assertion for $.achievements since it's @JsonIgnore'd in
        // UserPublicDTO
    }

    // Test for PUT /api/users/me
    @Test
    public void updateMyProfile_validInput_returnsUpdatedUser() throws Exception {
        // given
        final String token = "Bearer test-token";
        User user = new User();
        // Set ID via reflection to 1L
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        user.setEmail("updated@example.com");
        user.setStatus(UserStatus.ONLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("updatedUser");
        profile.setStatsPublic(false);
        user.setProfile(profile);

        // Expected response DTO after update
        UserMeDTO responseDTO = new UserMeDTO();
        responseDTO.setUserid(1L);
        responseDTO.setUsername(profile.getUsername());
        responseDTO.setEmail(user.getEmail());

        // Build update request DTO
        UserMeDTO updateRequestDTO = new UserMeDTO();
        updateRequestDTO.setUsername("updatedUser");
        updateRequestDTO.setEmail("updated@example.com");
        updateRequestDTO.setStatsPublic(false);

        given(userService.updateMyUser(eq(token), eq("updatedUser"), eq("updated@example.com"), eq(false)))
                .willReturn(user);
        given(mapper.toUserMeDTO(user)).willReturn(responseDTO);

        // when/then

        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(asJsonString(updateRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid", is(1)))
                .andExpect(jsonPath("$.username", is("updatedUser")))
                .andExpect(jsonPath("$.email", is("updated@example.com")));
    }

    @Test
    public void searchUsers_validEmail_returnsFoundUser() throws Exception {
        // given
        final String token = "Bearer test-token";
        String searchEmail = "search@example.com";

        User userToFind = new User();
        // Set ID via reflection
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userToFind, 2L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userToFind.setEmail(searchEmail);
        userToFind.setStatus(UserStatus.ONLINE);

        UserProfile profile = new UserProfile();
        profile.setUsername("searchUser");
        userToFind.setProfile(profile);

        // Create search request DTO
        UserSearchRequestDTO searchRequestDTO = new UserSearchRequestDTO();
        searchRequestDTO.setEmail(searchEmail);

        // Create expected response DTO
        UserSearchResponseDTO searchResponseDTO = new UserSearchResponseDTO();
        searchResponseDTO.setUserid(2L);
        searchResponseDTO.setUsername("searchUser");
        searchResponseDTO.setEmail(searchEmail);

        // Mock authentication service to return valid user for token verification
        User authUser = new User();
        given(authService.getUserByToken(eq(token))).willReturn(authUser);

        // Mock user service to return the found user
        given(userService.searchUserByEmail(eq(searchEmail))).willReturn(userToFind);

        // Mock mapper to return the expected DTO
        given(mapper.toUserSearchResponseDTO(userToFind)).willReturn(searchResponseDTO);

        // when/then
        mockMvc.perform(post("/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(asJsonString(searchRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userid", is(2)))
                .andExpect(jsonPath("$.username", is("searchUser")))
                .andExpect(jsonPath("$.email", is(searchEmail)));
    }

    // Test for DELETE /api/users/me
    @Test
    public void deleteMyAccount_validInput_deletesUserAccount() throws Exception {
        // given
        final String token = "Bearer test-token";
        String password = "correct-password";

        // Create delete request DTO
        UserDeleteRequestDTO deleteRequestDTO = new UserDeleteRequestDTO();
        deleteRequestDTO.setPassword(password);

        // Mock service call: nothing to return since it’s void
        // We can verify interaction in advanced cases using Mockito.verify
        // For now, just assume it works without throwing

        // when/then
        mockMvc.perform(delete("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(asJsonString(deleteRequestDTO)))
                .andExpect(status().isNoContent());
    }

    // Helper Method: converts a Java object into JSON string.
    private String asJsonString(final Object object) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}