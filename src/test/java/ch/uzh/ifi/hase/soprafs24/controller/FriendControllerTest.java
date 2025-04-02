package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FriendController.class)
public class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendService friendService;

    @MockBean
    private DTOMapper mapper;

    private FriendRequestDTO friendRequestDTO;
    private FriendRequest friendRequest;
    private FriendDTO friendDTO;
    private User user;
    private List<FriendRequest> friendRequests;
    private List<FriendDTO> friends;

    @BeforeEach
    void setUp() {
        // Initialize FriendRequestDTO
        friendRequestDTO = new FriendRequestDTO();
        friendRequestDTO.setRecipient(2L); // Recipient user ID
        friendRequestDTO.setAction("accept");

        // Initialize FriendRequest
        friendRequest = new FriendRequest();
        friendRequest.setStatus(FriendRequestStatus.PENDING);

        // Initialize FriendDTO
        friendDTO = new FriendDTO();
        friendDTO.setFriendId(1L);
        friendDTO.setUsername("friendUsername");

        // Initialize User
        user = new User();
        user.setEmail("userEmail@example.com");
        user.setPassword("password");
        user.setStatus(UserStatus.ONLINE);
        user.setCreatedAt(Instant.now());

        // Initialize lists for collections
        friendRequests = new ArrayList<>();
        friendRequests.add(friendRequest);

        friends = new ArrayList<>();
        friends.add(friendDTO);
    }

    @Test
    public void getFriendRequests_ShouldReturnIncomingFriendRequests() throws Exception {
        when(friendService.getIncomingFriendRequests("token")).thenReturn(friendRequests);
        when(mapper.toFriendRequestDTO(friendRequest)).thenReturn(friendRequestDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/friends/requests")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].recipient").value(2)); // Verify recipient ID
    }

    @Test
    public void sendFriendRequest_ShouldSendFriendRequestAndReturnCreated() throws Exception {
        when(friendService.sendFriendRequest("token", 2L)).thenReturn(friendRequest);
        when(mapper.toFriendRequestDTO(friendRequest)).thenReturn(friendRequestDTO);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/friends/request")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\":2}")) // Use recipient user ID
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipient").value(2)); // Verify recipient ID
    }

    @Test
    public void respondToFriendRequest_ShouldRespondToFriendRequestAndReturnOk() throws Exception {
        when(friendService.respondToFriendRequest("token", 1L, "accept")).thenReturn(friendRequest);
        when(mapper.toFriendRequestDTO(friendRequest)).thenReturn(friendRequestDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/friends/requests/1")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"accept\"}"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipient").value(2)); // Verify recipient ID
    }

    @Test
    public void listFriends_ShouldReturnListOfFriends() throws Exception {
        when(friendService.getFriends("token")).thenReturn(List.of(user));
        when(mapper.toFriendDTO(user)).thenReturn(friendDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/friends")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].friendId").value(1)); // Verify friend ID
    }

    @Test
    public void unfriend_ShouldUnfriendUserAndReturnNoContent() throws Exception {
        doNothing().when(friendService).unfriend("token", 1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/friends/1")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}