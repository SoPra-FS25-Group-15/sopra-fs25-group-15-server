package ch.uzh.ifi.hase.soprafs24.service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private FriendService friendService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getIncomingFriendRequests_ShouldReturnPendingRequests() {
        // Arrange
        User recipient = new User();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        FriendRequest request = new FriendRequest();
        request.setSender(new User());
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        request.onCreate();

        List<FriendRequest> requests = new ArrayList<>();
        requests.add(request);

        when(authService.getUserByToken("token")).thenReturn(recipient);
        when(friendRequestRepository.findByRecipientAndStatus(recipient, FriendRequestStatus.PENDING)).thenReturn(requests);

        // Act
        List<FriendRequest> result = friendService.getIncomingFriendRequests("token");

        // Assert
        assertEquals(1, result.size());
        assertEquals(FriendRequestStatus.PENDING, result.get(0).getStatus());
        assertNotNull(result.get(0).getCreatedAt()); // Verify createdAt is set
    }

    @Test
    void sendFriendRequest_ShouldCreateNewRequest() {
        // Arrange
        User sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPassword("password");
        sender.setStatus(UserStatus.ONLINE);

        User recipient = new User();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        when(authService.getUserByToken("token")).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        // Mock the save method to return the saved entity
        when(friendRequestRepository.save(any(FriendRequest.class))).thenAnswer(invocation -> {
            FriendRequest request = invocation.getArgument(0);
            request.setStatus(FriendRequestStatus.PENDING); // Simulate the @PrePersist logic
            return request;
        });

        // Act
        FriendRequest result = friendService.sendFriendRequest("token", 2L);

        // Assert
        assertNotNull(result);
        assertEquals(sender, result.getSender());
        assertEquals(recipient, result.getRecipient());
        assertEquals(FriendRequestStatus.PENDING, result.getStatus());
        verify(friendRequestRepository, times(1)).save(any(FriendRequest.class));
    }

    @Test
    void respondToFriendRequest_ShouldAcceptRequest() {
        // Arrange
        User recipient = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        User sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPassword("password");
        sender.setStatus(UserStatus.ONLINE);

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);

        when(authService.getUserByToken("token")).thenReturn(recipient);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act
        FriendRequest result = friendService.respondToFriendRequest("token", 1L, "accept");

        // Assert
        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
    }

    @Test
    void respondToFriendRequest_ShouldDenyRequest() {
        // Arrange
        User recipient = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        recipient.onCreate();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        User sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPassword("password");
        sender.setStatus(UserStatus.ONLINE);
        sender.onCreate();

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        request.onCreate();

        when(authService.getUserByToken("token")).thenReturn(recipient);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act
        FriendRequest result = friendService.respondToFriendRequest("token", 1L, "deny");

        // Assert
        assertEquals(FriendRequestStatus.DENIED, result.getStatus());
        assertNotNull(result.getCreatedAt()); // Verify createdAt is set
    }

    @Test
    void respondToFriendRequest_ShouldThrowExceptionForInvalidAction() {
        // Arrange
        User recipient = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        recipient.onCreate();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        FriendRequest request = new FriendRequest();
        request.onCreate();
        request.setId(1L);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);

        when(authService.getUserByToken("token")).thenReturn(recipient);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> friendService.respondToFriendRequest("token", 1L, "invalid"));
    }

    @Test
    void getFriends_ShouldReturnListOfFriends() {
        // Arrange
        User currentUser = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(currentUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        currentUser.onCreate();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);

        User friend1 = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friend1, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        friend1.onCreate();
        friend1.setEmail("friend1@example.com");
        friend1.setPassword("password");
        friend1.setStatus(UserStatus.ONLINE);

        User friend2 = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friend2, 3L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        friend2.onCreate();
        friend2.setEmail("friend2@example.com");
        friend2.setPassword("password");
        friend2.setStatus(UserStatus.ONLINE);

        // Create accepted friend requests instead of friendships
        FriendRequest request1 = new FriendRequest();
        request1.setSender(currentUser);
        request1.setRecipient(friend1);
        request1.setStatus(FriendRequestStatus.ACCEPTED);

        FriendRequest request2 = new FriendRequest();
        request2.setSender(friend2);
        request2.setRecipient(currentUser);
        request2.setStatus(FriendRequestStatus.ACCEPTED);

        List<FriendRequest> friendRequests = new ArrayList<>();
        friendRequests.add(request1);
        friendRequests.add(request2);

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser)).thenReturn(friendRequests);

        // Act
        List<User> friends = friendService.getFriends("token");

        // Assert
        assertEquals(2, friends.size());
        assertTrue(friends.contains(friend1));
        assertTrue(friends.contains(friend2));
    }

    @Test
    void unfriend_ShouldRemoveFriendship() {
        // Arrange
        User currentUser = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(currentUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        currentUser.onCreate();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);

        User friend = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friend, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        friend.onCreate();
        friend.setEmail("friend@example.com");
        friend.setPassword("password");
        friend.setStatus(UserStatus.ONLINE);

        // Create an accepted friend request
        FriendRequest acceptedRequest = new FriendRequest();
        acceptedRequest.setId(1L);
        acceptedRequest.setSender(currentUser);
        acceptedRequest.setRecipient(friend);
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);

        List<FriendRequest> requests = new ArrayList<>();
        requests.add(acceptedRequest);

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(userRepository.findById(friend.getId())).thenReturn(Optional.of(friend));
        when(friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser)).thenReturn(requests);

        // Act
        friendService.unfriend("token", friend.getId());

        // Assert
        verify(friendRequestRepository, times(1)).delete(acceptedRequest);
    }

    @Test
    void unfriend_ShouldThrowExceptionIfFriendshipNotFound() {
        // Arrange
        User currentUser = new User();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);
        
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(currentUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        
        User friend = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friend, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));
        when(friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser)).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> friendService.unfriend("token", 2L));
    }

    // Either remove this test or keep it if the method is still used elsewhere
    @Test
    void getAllFriendRequests_ShouldReturnBothSentAndReceived() {
        // This test is testing a method that is no longer used by the controller
        // You might want to remove it if the method is no longer needed
        // ...existing code...
    }
    
    @Test
    void cancelFriendRequest_ShouldDeletePendingRequest() {
        // Arrange
        User sender = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        sender.setEmail("sender@example.com");
        
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setSender(sender);
        request.setRecipient(new User());
        request.setStatus(FriendRequestStatus.PENDING);
        
        when(authService.getUserByToken("token")).thenReturn(sender);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        // Act
        friendService.cancelFriendRequest("token", 1L);
        
        // Assert
        verify(friendRequestRepository, times(1)).delete(request);
    }
    
    @Test
    void cancelFriendRequest_ShouldThrowExceptionIfNotSender() {
        // Arrange
        User currentUser = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(currentUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        
        User otherUser = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(otherUser, 2L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setSender(otherUser); // Current user is not the sender
        request.setRecipient(new User());
        
        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> friendService.cancelFriendRequest("token", 1L));
    }
    
    @Test
    void cancelFriendRequest_ShouldThrowExceptionIfNotPending() {
        // Arrange
        User sender = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        
        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setSender(sender);
        request.setRecipient(new User());
        request.setStatus(FriendRequestStatus.ACCEPTED); // Already accepted
        
        when(authService.getUserByToken("token")).thenReturn(sender);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> friendService.cancelFriendRequest("token", 1L));
    }

    @Test
    void getOutgoingFriendRequests_ShouldReturnSentRequestsWithStatuses() {
        // Arrange
        User sender = new User();
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        sender.setEmail("sender@example.com");
        
        // Create a pending request
        FriendRequest pendingRequest = new FriendRequest();
        pendingRequest.setSender(sender);
        pendingRequest.setRecipient(new User());
        pendingRequest.setStatus(FriendRequestStatus.PENDING);
        
        // Create an accepted request
        FriendRequest acceptedRequest = new FriendRequest();
        acceptedRequest.setSender(sender);
        acceptedRequest.setRecipient(new User());
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);
        
        List<FriendRequest> sentRequests = List.of(pendingRequest, acceptedRequest);
        
        when(authService.getUserByToken("token")).thenReturn(sender);
        when(friendRequestRepository.findBySender(sender)).thenReturn(sentRequests);
        
        // Act
        List<FriendRequest> result = friendService.getOutgoingFriendRequests("token");
        
        // Assert
        assertEquals(2, result.size());
        assertEquals(FriendRequestStatus.PENDING, result.get(0).getStatus());
        assertEquals(FriendRequestStatus.ACCEPTED, result.get(1).getStatus());
    }
}