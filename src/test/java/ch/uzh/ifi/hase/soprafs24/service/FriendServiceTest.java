package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FriendServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

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
        recipient.setId(1L);
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

        // Mock the save method to return the saved entity with an ID
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L); // Simulate the ID being set by the database
            return user;
        });

        when(authService.getUserByToken("token")).thenReturn(recipient);
        when(friendRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        // Act
        FriendRequest result = friendService.respondToFriendRequest("token", 1L, "accept");

        // Assert
        assertEquals(FriendRequestStatus.ACCEPTED, result.getStatus());
        verify(friendshipRepository, times(1)).save(any(Friendship.class));
    }

    @Test
    void respondToFriendRequest_ShouldDenyRequest() {
        // Arrange
        User recipient = new User();
        recipient.setId(1L);
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
        recipient.setId(1L);
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
        currentUser.setId(1L);
        currentUser.onCreate();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);

        User friend1 = new User();
        friend1.setId(2L);
        friend1.onCreate();
        friend1.setEmail("friend1@example.com");
        friend1.setPassword("password");
        friend1.setStatus(UserStatus.ONLINE);

        User friend2 = new User();
        friend2.setId(3L);
        friend2.onCreate();
        friend2.setEmail("friend2@example.com");
        friend2.setPassword("password");
        friend2.setStatus(UserStatus.ONLINE);

        Friendship friendship1 = new Friendship();
        friendship1.setUser1(currentUser);
        friendship1.setUser2(friend1);

        Friendship friendship2 = new Friendship();
        friendship2.setUser1(friend2);
        friendship2.setUser2(currentUser);

        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship1);
        friendships.add(friendship2);

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(friendshipRepository.findAll()).thenReturn(friendships);

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
        currentUser.setId(1L);
        currentUser.onCreate();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);

        User friend = new User();
        friend.setId(2L);
        friend.onCreate();
        friend.setEmail("friend@example.com");
        friend.setPassword("password");
        friend.setStatus(UserStatus.ONLINE);

        Friendship friendship = new Friendship();
        friendship.setUser1(currentUser);
        friendship.setUser2(friend);

        List<Friendship> friendships = new ArrayList<>();
        friendships.add(friendship);

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(friendshipRepository.findAll()).thenReturn(friendships);
        System.out.println(friend.getId());
        // Act
        friendService.unfriend("token", friend.getId());

        // Assert
        verify(friendshipRepository, times(1)).delete(friendship);
    }

    @Test
    void unfriend_ShouldThrowExceptionIfFriendshipNotFound() {
        // Arrange
        User currentUser = new User();
        currentUser.setEmail("current@example.com");
        currentUser.setPassword("password");
        currentUser.setStatus(UserStatus.ONLINE);

        when(authService.getUserByToken("token")).thenReturn(currentUser);
        when(friendshipRepository.findAll()).thenReturn(new ArrayList<>());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> friendService.unfriend("token", 2L));
    }
}