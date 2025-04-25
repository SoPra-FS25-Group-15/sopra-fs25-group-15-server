package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private FriendService friendService;

    private User testUser1;
    private User testUser2;
    private FriendRequest pendingRequest;
    private FriendRequest acceptedRequest;
    private final String TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test users
        testUser1 = new User();
        testUser1.setId(1L);

        testUser2 = new User();
        testUser2.setId(2L);

        // Setup test friend requests
        pendingRequest = new FriendRequest();
        pendingRequest.setId(1L);
        pendingRequest.setSender(testUser1);
        pendingRequest.setRecipient(testUser2);
        pendingRequest.setStatus(FriendRequestStatus.PENDING);

        acceptedRequest = new FriendRequest();
        acceptedRequest.setId(2L);
        acceptedRequest.setSender(testUser1);
        acceptedRequest.setRecipient(testUser2);
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);

        // Setup mock behaviors
        when(authService.getUserByToken(TOKEN)).thenReturn(testUser1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser2));
    }

    @Test
    void getIncomingFriendRequests_success() {
        List<FriendRequest> requests = Arrays.asList(pendingRequest);
        when(friendRequestRepository.findByRecipientAndStatus(testUser1, FriendRequestStatus.PENDING))
            .thenReturn(requests);

        List<FriendRequest> result = friendService.getIncomingFriendRequests(TOKEN);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(pendingRequest, result.get(0));
    }

    @Test
    void sendFriendRequest_success() {
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(i -> i.getArguments()[0]);

        FriendRequest result = friendService.sendFriendRequest(TOKEN, 2L);

        assertNotNull(result);
        assertEquals(testUser1, result.getSender());
        assertEquals(testUser2, result.getRecipient());
        verify(friendRequestRepository).save(any(FriendRequest.class));
    }

    @Test
    void sendFriendRequest_recipientNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            friendService.sendFriendRequest(TOKEN, 99L);
        });
    }

    @Test
    void respondToFriendRequest_accept() {
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);

        // Set testUser2 as the current user (recipient)
        when(authService.getUserByToken(TOKEN)).thenReturn(testUser2);

        FriendRequest result = friendService.respondToFriendRequest(TOKEN, 1L, "accept");

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void respondToFriendRequest_deny() {
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);

        // Set testUser2 as the current user (recipient)
        when(authService.getUserByToken(TOKEN)).thenReturn(testUser2);

        FriendRequest result = friendService.respondToFriendRequest(TOKEN, 1L, "deny");

        assertEquals(FriendRequestStatus.DENIED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void respondToFriendRequest_unauthorized() {
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        // testUser1 is not the recipient
        assertThrows(ResponseStatusException.class, () -> {
            friendService.respondToFriendRequest(TOKEN, 1L, "accept");
        });
    }

    @Test
    void getFriends_success() {
        List<FriendRequest> requests = Arrays.asList(acceptedRequest);
        when(friendRequestRepository.findBySenderOrRecipient(testUser1, testUser1)).thenReturn(requests);

        List<User> result = friendService.getFriends(TOKEN);

        assertEquals(1, result.size());
        assertEquals(testUser2, result.get(0));
    }

    @Test
    void unfriend_success() {
        List<FriendRequest> requests = Arrays.asList(acceptedRequest);
        when(friendRequestRepository.findBySenderOrRecipient(testUser1, testUser1)).thenReturn(requests);

        friendService.unfriend(TOKEN, 2L);

        verify(friendRequestRepository).delete(acceptedRequest);
    }

    @Test
    void unfriend_notFound() {
        when(friendRequestRepository.findBySenderOrRecipient(testUser1, testUser1)).thenReturn(new ArrayList<>());

        assertThrows(ResponseStatusException.class, () -> {
            friendService.unfriend(TOKEN, 2L);
        });
    }

    @Test
    void cancelFriendRequest_success() {
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        friendService.cancelFriendRequest(TOKEN, 1L);

        verify(friendRequestRepository).delete(pendingRequest);
    }

    @Test
    void cancelFriendRequest_notSender() {
        // Make testUser2 the sender instead of testUser1
        pendingRequest.setSender(testUser2);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(ResponseStatusException.class, () -> {
            friendService.cancelFriendRequest(TOKEN, 1L);
        });
    }

    @Test
    void cancelFriendRequest_notPending() {
        when(friendRequestRepository.findById(2L)).thenReturn(Optional.of(acceptedRequest));

        assertThrows(ResponseStatusException.class, () -> {
            friendService.cancelFriendRequest(TOKEN, 2L);
        });
    }

    @Test
    void getOutgoingFriendRequests_success() {
        List<FriendRequest> requests = Arrays.asList(pendingRequest);
        when(friendRequestRepository.findBySender(testUser1)).thenReturn(requests);

        List<FriendRequest> result = friendService.getOutgoingFriendRequests(TOKEN);

        assertEquals(1, result.size());
        assertEquals(pendingRequest, result.get(0));
    }

    @Test
    void respondToFriendRequestBySender_success() {
        when(friendRequestRepository.findBySenderAndRecipientAndStatus(
            eq(testUser2), eq(testUser1), eq(FriendRequestStatus.PENDING)
        )).thenReturn(pendingRequest);
        when(friendRequestRepository.save(any(FriendRequest.class))).thenReturn(pendingRequest);

        FriendRequest result = friendService.respondToFriendRequestBySender(TOKEN, 2L, "accept");

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }
}