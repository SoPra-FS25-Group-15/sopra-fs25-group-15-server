package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@WebMvcTest(FriendController.class)
public class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FriendService friendService;

    @MockBean
    private DTOMapper mapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

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
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
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

        mockMvc.perform(MockMvcRequestBuilders.get("/friends/requests")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].recipient").value(2)); // Verify recipient ID
    }

    @Test
    public void sendFriendRequest_ShouldSendFriendRequestAndReturnCreated() throws Exception {
        // Create a recipient user with id 2 and email recipient@example.com
        User recipient = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);
        recipient.setCreatedAt(Instant.now());
        recipient.setProfile(new UserProfile());
        recipient.getProfile().setUsername("recipientUsername");

        when(userService.getUserBySearch("recipient@example.com")).thenReturn(recipient);
        when(authService.getUserByToken("token")).thenReturn(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/friends/request")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"recipient@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipient").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipientUsername").value("recipientUsername"));
    }

    @Test
    public void respondToFriendRequest_ShouldRespondToFriendRequestAndReturnOk() throws Exception {
        when(friendService.respondToFriendRequest("token", 1L, "accept")).thenReturn(friendRequest);
        when(mapper.toFriendRequestDTO(friendRequest)).thenReturn(friendRequestDTO);

        mockMvc.perform(MockMvcRequestBuilders.put("/friends/requests/1")
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

        mockMvc.perform(MockMvcRequestBuilders.get("/friends")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].friendId").value(1)); // Verify friend ID
    }

    @Test
    public void unfriend_ShouldUnfriendUserAndReturnNoContent() throws Exception {
        doNothing().when(friendService).unfriend("token", 1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/friends/1")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void cancelFriendRequest_ShouldCancelRequestAndReturnNoContent() throws Exception {
        doNothing().when(friendService).cancelFriendRequest("token", 1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/friends/requests/1")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(friendService, times(1)).cancelFriendRequest("token", 1L);
    }

    @Test
    public void getAllFriendRequests_ShouldReturnOnlyOutgoingRequests() throws Exception {
        // Create test data - only outgoing requests now
        FriendRequest pendingRequest = new FriendRequest();
        pendingRequest.setId(1L);
        pendingRequest.setStatus(FriendRequestStatus.PENDING);

        FriendRequest acceptedRequest = new FriendRequest();
        acceptedRequest.setId(2L);
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);

        List<FriendRequest> outgoingRequests = List.of(pendingRequest, acceptedRequest);

        // Mock services - using getOutgoingFriendRequests as implemented in the
        // controller
        when(friendService.getOutgoingFriendRequests("token")).thenReturn(outgoingRequests);
        when(authService.getUserByToken("token")).thenReturn(user);

        // Mock mapper to return appropriate DTOs with status information
        FriendRequestDTO pendingDTO = new FriendRequestDTO();
        pendingDTO.setRequestId(1L);
        pendingDTO.setStatus("pending");
        pendingDTO.setIncoming(false);

        FriendRequestDTO acceptedDTO = new FriendRequestDTO();
        acceptedDTO.setRequestId(2L);
        acceptedDTO.setStatus("accepted");
        acceptedDTO.setIncoming(false);

        when(mapper.toFriendRequestDTO(eq(pendingRequest), eq(user))).thenReturn(pendingDTO);
        when(mapper.toFriendRequestDTO(eq(acceptedRequest), eq(user))).thenReturn(acceptedDTO);

        // Perform request
        mockMvc.perform(MockMvcRequestBuilders.get("/friends/all-requests")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].requestId").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("pending"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].requestId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].status").value("accepted"));
    }
}