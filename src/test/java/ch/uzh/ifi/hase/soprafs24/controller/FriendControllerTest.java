package ch.uzh.ifi.hase.soprafs24.controller;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
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
    private User recipient;
    private List<FriendRequest> friendRequests;
    private List<FriendDTO> friends;

    @BeforeEach
    void setUp() {
        // Initialize FriendRequestDTO
        friendRequestDTO = new FriendRequestDTO();
        friendRequestDTO.setRequestId(1L);
        friendRequestDTO.setRecipient(2L); // Recipient user ID
        friendRequestDTO.setAction("accept");
        friendRequestDTO.setStatus("pending");
        friendRequestDTO.setIncoming(true);
        friendRequestDTO.setRecipientUsername("recipientUsername");

        // Initialize FriendRequest
        friendRequest = new FriendRequest();
        friendRequest.setId(1L);
        friendRequest.setStatus(FriendRequestStatus.PENDING);

        // Initialize FriendDTO
        friendDTO = new FriendDTO();
        friendDTO.setFriendId(2L);
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

        // Create a profile for the user
        UserProfile userProfile = new UserProfile();
        userProfile.setUsername("testUsername");
        user.setProfile(userProfile);

        // Initialize recipient user
        recipient = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set recipient ID via reflection", e);
        }
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);
        recipient.setCreatedAt(Instant.now());

        // Create a profile for the recipient
        UserProfile recipientProfile = new UserProfile();
        recipientProfile.setUsername("recipientUsername");
        recipient.setProfile(recipientProfile);

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
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].recipient").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("pending"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].incoming").value(true));
    }

    @Test
    public void getFriendRequests_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        when(friendService.getIncomingFriendRequests("invalid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(MockMvcRequestBuilders.get("/friends/requests")
                        .header("Authorization", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void sendFriendRequest_ShouldSendFriendRequestAndReturnCreated() throws Exception {
        when(userService.getUserBySearch("recipient@example.com")).thenReturn(recipient);
        when(authService.getUserByToken("token")).thenReturn(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/friends/request")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"recipient@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipient").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipientUsername").value("recipientUsername"));

        verify(friendService, times(1)).sendFriendRequest("token", 2L);
    }

    @Test
    public void sendFriendRequest_ToSelf_ShouldReturnBadRequest() throws Exception {
        when(userService.getUserBySearch("userEmail@example.com")).thenReturn(user);
        when(authService.getUserByToken("token")).thenReturn(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/friends/request")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"userEmail@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void sendFriendRequest_UserNotFound_ShouldReturnNotFound() throws Exception {
        when(userService.getUserBySearch("nonexistent@example.com"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(MockMvcRequestBuilders.post("/friends/request")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"nonexistent@example.com\"}"))
                .andExpect(status().isNotFound());
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.recipient").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("pending"));
    }

    @Test
    public void respondToFriendRequest_InvalidRequestId_ShouldReturnNotFound() throws Exception {
        when(friendService.respondToFriendRequest("token", 999L, "accept"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));

        mockMvc.perform(MockMvcRequestBuilders.put("/friends/requests/999")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"accept\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void respondToFriendRequest_InvalidAction_ShouldReturnBadRequest() throws Exception {
        when(friendService.respondToFriendRequest("token", 1L, "invalid-action"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action"));

        mockMvc.perform(MockMvcRequestBuilders.put("/friends/requests/1")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"invalid-action\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void listFriends_ShouldReturnListOfFriends() throws Exception {
        when(friendService.getFriends("token")).thenReturn(List.of(recipient));
        when(mapper.toFriendDTO(recipient)).thenReturn(friendDTO);

        mockMvc.perform(MockMvcRequestBuilders.get("/friends")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].friendId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].username").value("friendUsername"));
    }

    @Test
    public void listFriends_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        when(friendService.getFriends("invalid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(MockMvcRequestBuilders.get("/friends")
                        .header("Authorization", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void unfriend_ShouldUnfriendUserAndReturnNoContent() throws Exception {
        doNothing().when(friendService).unfriend("token", 2L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/friends/2")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(friendService, times(1)).unfriend("token", 2L);
    }

    @Test
    public void unfriend_WithNonExistingFriend_ShouldReturnNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend relationship not found"))
                .when(friendService).unfriend("token", 999L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/friends/999")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
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
    public void cancelFriendRequest_NonExistingRequest_ShouldReturnNotFound() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"))
                .when(friendService).cancelFriendRequest("token", 999L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/friends/requests/999")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getAllFriendRequests_ShouldReturnBothIncomingAndOutgoingRequests() throws Exception {
        // Create test data
        FriendRequest outgoingRequest = new FriendRequest();
        outgoingRequest.setId(1L);
        outgoingRequest.setStatus(FriendRequestStatus.PENDING);

        FriendRequest incomingRequest = new FriendRequest();
        incomingRequest.setId(2L);
        incomingRequest.setStatus(FriendRequestStatus.PENDING);

        List<FriendRequest> outgoingRequests = List.of(outgoingRequest);
        List<FriendRequest> incomingRequests = List.of(incomingRequest);

        // Mock the service calls
        when(friendService.getOutgoingFriendRequests("token")).thenReturn(outgoingRequests);
        when(friendService.getIncomingFriendRequests("token")).thenReturn(incomingRequests);
        when(authService.getUserByToken("token")).thenReturn(user);

        // Create DTOs for the outgoing and incoming requests
        FriendRequestDTO outgoingDTO = new FriendRequestDTO();
        outgoingDTO.setRequestId(1L);
        outgoingDTO.setStatus("pending");
        outgoingDTO.setIncoming(false);

        FriendRequestDTO incomingDTO = new FriendRequestDTO();
        incomingDTO.setRequestId(2L);
        incomingDTO.setStatus("pending");
        incomingDTO.setIncoming(true);

        when(mapper.toFriendRequestDTO(eq(outgoingRequest), eq(user))).thenReturn(outgoingDTO);
        when(mapper.toFriendRequestDTO(eq(incomingRequest), eq(user))).thenReturn(incomingDTO);

        // Perform the request
        mockMvc.perform(MockMvcRequestBuilders.get("/friends/all-requests")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].requestId").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].incoming").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].requestId").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].incoming").value(true));
    }

    @Test
    public void getAllFriendRequests_WithInvalidToken_ShouldReturnUnauthorized() throws Exception {
        when(friendService.getOutgoingFriendRequests("invalid-token"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(MockMvcRequestBuilders.get("/friends/all-requests")
                        .header("Authorization", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}