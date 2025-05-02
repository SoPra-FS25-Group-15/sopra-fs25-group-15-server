package ch.uzh.ifi.hase.soprafs24.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@SpringBootTest(properties = {
    "spring.cloud.gcp.sql.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.show-sql=true",
    "jwt.secret=test-secret",
    "google.maps.api.key=TEST_KEY"
})
@Transactional
@AutoConfigureTestDatabase(replace = ANY)
public class FriendServiceTest {

    private static final String TOKEN = "test-token";

    @Autowired
    private FriendService friendService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FriendRequestRepository friendRequestRepository;

    private User user1;
    private User user2;
    private FriendRequest pendingRequest;
    private FriendRequest acceptedRequest;

    @BeforeEach
    void setUp() {
        user1 = new User(); user1.setId(1L);
        user2 = new User(); user2.setId(2L);

        when(authService.getUserByToken(TOKEN)).thenReturn(user1);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        pendingRequest = new FriendRequest();
        pendingRequest.setId(1L);
        pendingRequest.setSender(user1);
        pendingRequest.setRecipient(user2);
        pendingRequest.setStatus(FriendRequestStatus.PENDING);

        acceptedRequest = new FriendRequest();
        acceptedRequest.setId(2L);
        acceptedRequest.setSender(user1);
        acceptedRequest.setRecipient(user2);
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);
    }

    @Test
    void getIncomingFriendRequests_success() {
        when(friendRequestRepository.findByRecipientAndStatus(
                user1, FriendRequestStatus.PENDING))
            .thenReturn(List.of(pendingRequest));

        List<FriendRequest> result = friendService.getIncomingFriendRequests(TOKEN);

        assertEquals(1, result.size());
        assertSame(pendingRequest, result.get(0));
    }

    @Test
    void getOutgoingFriendRequests_success() {
        when(friendRequestRepository.findBySender(user1))
            .thenReturn(List.of(pendingRequest));

        List<FriendRequest> result = friendService.getOutgoingFriendRequests(TOKEN);

        assertEquals(1, result.size());
        assertSame(pendingRequest, result.get(0));
    }

    @Test
    void sendFriendRequest_success() {
        when(friendRequestRepository.save(any(FriendRequest.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        FriendRequest result = friendService.sendFriendRequest(TOKEN, 2L);

        assertNotNull(result);
        assertEquals(user1, result.getSender());
        assertEquals(user2, result.getRecipient());
        verify(friendRequestRepository).save(result);
    }

    @Test
    void sendFriendRequest_recipientNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                     () -> friendService.sendFriendRequest(TOKEN, 99L));
    }

    @Test
    void respondToFriendRequest_accept_success() {
        when(friendRequestRepository.findById(1L))
            .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class)))
            .thenReturn(pendingRequest);

        // switch to recipient
        when(authService.getUserByToken(TOKEN)).thenReturn(user2);

        FriendRequest result = friendService.respondToFriendRequest(TOKEN, 1L, "accept");

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void respondToFriendRequest_deny_success() {
        when(friendRequestRepository.findById(1L))
            .thenReturn(Optional.of(pendingRequest));
        when(friendRequestRepository.save(any(FriendRequest.class)))
            .thenReturn(pendingRequest);

        when(authService.getUserByToken(TOKEN)).thenReturn(user2);

        FriendRequest result = friendService.respondToFriendRequest(TOKEN, 1L, "deny");

        assertEquals(FriendRequestStatus.DENIED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void respondToFriendRequest_unauthorized_throws() {
        when(friendRequestRepository.findById(1L))
            .thenReturn(Optional.of(pendingRequest));
        // still sender as caller
        when(authService.getUserByToken(TOKEN)).thenReturn(user1);

        assertThrows(ResponseStatusException.class,
                     () -> friendService.respondToFriendRequest(TOKEN, 1L, "accept"));
    }

    @Test
    void respondToFriendRequestBySender_accept_success() {
        // FIXED: swap sender/recipient to match service call
        when(friendRequestRepository.findBySenderAndRecipientAndStatus(
                user2,   // the "senderId" passed in test
                user1,   // the token-user is always treated as recipient here
                FriendRequestStatus.PENDING))
            .thenReturn(pendingRequest);

        when(friendRequestRepository.save(any(FriendRequest.class)))
            .thenReturn(pendingRequest);

        FriendRequest result =
            friendService.respondToFriendRequestBySender(TOKEN, 2L, "accept");

        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void respondToFriendRequestBySender_deny_success() {
        // FIXED: swap sender/recipient here as well
        when(friendRequestRepository.findBySenderAndRecipientAndStatus(
                user2,
                user1,
                FriendRequestStatus.PENDING))
            .thenReturn(pendingRequest);

        when(friendRequestRepository.save(any(FriendRequest.class)))
            .thenReturn(pendingRequest);

        FriendRequest result =
            friendService.respondToFriendRequestBySender(TOKEN, 2L, "deny");

        assertEquals(FriendRequestStatus.DENIED, result.getStatus());
        verify(friendRequestRepository).save(pendingRequest);
    }

    @Test
    void cancelFriendRequest_success() {
        when(friendRequestRepository.findById(1L))
            .thenReturn(Optional.of(pendingRequest));

        friendService.cancelFriendRequest(TOKEN, 1L);

        verify(friendRequestRepository).delete(pendingRequest);
    }

    @Test
    void cancelFriendRequest_notSender_throws() {
        // make user2 the sender
        pendingRequest.setSender(user2);
        when(friendRequestRepository.findById(1L))
            .thenReturn(Optional.of(pendingRequest));

        assertThrows(ResponseStatusException.class,
                     () -> friendService.cancelFriendRequest(TOKEN, 1L));
    }

    @Test
    void cancelFriendRequest_notPending_throws() {
        FriendRequest nonPending = new FriendRequest();
        nonPending.setId(3L);
        nonPending.setSender(user1);
        nonPending.setRecipient(user2);
        nonPending.setStatus(FriendRequestStatus.ACCEPTED);
        when(friendRequestRepository.findById(3L))
            .thenReturn(Optional.of(nonPending));

        assertThrows(ResponseStatusException.class,
                     () -> friendService.cancelFriendRequest(TOKEN, 3L));
    }

    @Test
    void unfriend_success() {
        when(friendRequestRepository.findBySenderOrRecipient(user1, user1))
            .thenReturn(List.of(acceptedRequest));

        friendService.unfriend(TOKEN, 2L);

        verify(friendRequestRepository).delete(acceptedRequest);
    }

    @Test
    void unfriend_notFound_throws() {
        when(friendRequestRepository.findBySenderOrRecipient(user1, user1))
            .thenReturn(Collections.emptyList());

        assertThrows(ResponseStatusException.class,
                     () -> friendService.unfriend(TOKEN, 2L));
    }

    @Test
    void getFriends_success() {
        when(friendRequestRepository.findBySenderOrRecipient(user1, user1))
            .thenReturn(List.of(acceptedRequest));

        List<User> friends = friendService.getFriends(TOKEN);

        assertEquals(1, friends.size());
        assertEquals(user2, friends.get(0));
    }
}
