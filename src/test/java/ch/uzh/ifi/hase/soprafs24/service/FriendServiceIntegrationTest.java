package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
@ActiveProfiles("test")
public class FriendServiceIntegrationTest {

    @Autowired
    private FriendService friendService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private GameRepository gameRepository;

    private User user1;
    private User user2;
    private User user3;
    private String token1;
    private String token2;
    private String token3;

    @BeforeEach
    public void setup() {
        friendRequestRepository.deleteAll();

        if (gameRepository != null) {
            gameRepository.findAll().forEach(game -> {
                game.setPlayers(null);
                gameRepository.save(game);
            });
            gameRepository.deleteAll();
        }
        userRepository.deleteAll();

        user1 = createTestUser("user1", "user1@example.com", "password1");
        user2 = createTestUser("user2", "user2@example.com", "password2");
        user3 = createTestUser("user3", "user3@example.com", "password3");


        token1 = loginUser("user1@example.com", "password1");
        token2 = loginUser("user2@example.com", "password2");
        token3 = loginUser("user3@example.com", "password3");
    }

    private User createTestUser(String username, String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setStatus(UserStatus.ONLINE);
        user.setUsername(username);

        return userRepository.save(user);
    }

    private String loginUser(String email, String password) {


        return authService.login(email, password).getToken();
    }

    @Test
    public void sendFriendRequest_Success() {

        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());

        assertNotNull(request);
        assertEquals(user1.getId(), request.getSender().getId());
        assertEquals(user2.getId(), request.getRecipient().getId());
        assertEquals(FriendRequestStatus.PENDING, request.getStatus());
    }

    @Test
    public void sendFriendRequest_RecipientNotFound_ThrowsException() {

        Long nonExistentId = 99999L;

        assertThrows(ResponseStatusException.class, () -> {
            friendService.sendFriendRequest(token1, nonExistentId);
        });
    }

    @Test
    public void getIncomingFriendRequests_Success() {
        friendService.sendFriendRequest(token1, user2.getId());
        List<FriendRequest> requests = friendService.getIncomingFriendRequests(token2);
        assertEquals(1, requests.size());
        assertEquals(user1.getId(), requests.get(0).getSender().getId());
        assertEquals(user2.getId(), requests.get(0).getRecipient().getId());
    }

    @Test
    public void getOutgoingFriendRequests_Success() {
        friendService.sendFriendRequest(token1, user2.getId());
        friendService.sendFriendRequest(token1, user3.getId());
        List<FriendRequest> requests = friendService.getOutgoingFriendRequests(token1);
        assertEquals(2, requests.size());
    }

    @Test
    public void acceptFriendRequest_Success() {
        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());
        FriendRequest acceptedRequest = friendService.respondToFriendRequest(token2, request.getId(), "accept");
        assertEquals(FriendRequestStatus.ACCEPTED, acceptedRequest.getStatus());
        List<User> user1Friends = friendService.getFriends(token1);
        List<User> user2Friends = friendService.getFriends(token2);
        assertEquals(1, user1Friends.size());
        assertEquals(1, user2Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());
        assertEquals(user1.getId(), user2Friends.get(0).getId());
    }

    @Test
    public void denyFriendRequest_Success() {
        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());
        FriendRequest deniedRequest = friendService.respondToFriendRequest(token2, request.getId(), "deny");
        assertEquals(FriendRequestStatus.DENIED, deniedRequest.getStatus());
        List<User> user1Friends = friendService.getFriends(token1);
        List<User> user2Friends = friendService.getFriends(token2);
        assertTrue(user1Friends.isEmpty());
        assertTrue(user2Friends.isEmpty());
    }

    @Test
    public void respondToFriendRequest_Unauthorized_ThrowsException() {

        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());


        assertThrows(ResponseStatusException.class, () -> {
            friendService.respondToFriendRequest(token3, request.getId(), "accept");
        });
    }

    @Test
    public void respondToFriendRequestBySender_Success() {

        friendService.sendFriendRequest(token1, user2.getId());


        FriendRequest acceptedRequest = friendService.respondToFriendRequestBySender(token2, user1.getId(), "accept");

        assertEquals(FriendRequestStatus.ACCEPTED, acceptedRequest.getStatus());


        List<User> user1Friends = friendService.getFriends(token1);
        assertEquals(1, user1Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());
    }

    @Test
    public void cancelFriendRequest_Success() {

        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());


        friendService.cancelFriendRequest(token1, request.getId());


        List<FriendRequest> outgoingRequests = friendService.getOutgoingFriendRequests(token1);
        assertTrue(outgoingRequests.isEmpty());
    }

    @Test
    public void cancelFriendRequestToUser_Success() {

        friendService.sendFriendRequest(token1, user2.getId());


        friendService.cancelFriendRequestToUser(token1, user2.getId());


        List<FriendRequest> outgoingRequests = friendService.getOutgoingFriendRequests(token1);
        assertTrue(outgoingRequests.isEmpty());
    }

    @Test
    public void cancelFriendRequest_NotSender_ThrowsException() {

        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());


        assertThrows(ResponseStatusException.class, () -> {
            friendService.cancelFriendRequest(token3, request.getId());
        });
    }

    @Test
    public void cancelFriendRequest_AlreadyAccepted_ThrowsException() {

        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());


        friendService.respondToFriendRequest(token2, request.getId(), "accept");


        assertThrows(ResponseStatusException.class, () -> {
            friendService.cancelFriendRequest(token1, request.getId());
        });
    }

    @Test
    public void unfriend_Success() {
        FriendRequest request = friendService.sendFriendRequest(token1, user2.getId());
        friendService.respondToFriendRequest(token2, request.getId(), "accept");
        List<User> user1Friends = friendService.getFriends(token1);
        assertEquals(1, user1Friends.size());
        friendService.unfriend(token1, user2.getId());
        user1Friends = friendService.getFriends(token1);
        assertTrue(user1Friends.isEmpty());
    }

    @Test
    public void unfriend_NotFriends_ThrowsException() {
        assertThrows(ResponseStatusException.class, () -> {
            friendService.unfriend(token1, user2.getId());
        });
    }

    @Test
    public void getFriends_MultipleFriends_Success() {

        FriendRequest request1 = friendService.sendFriendRequest(token1, user2.getId());
        friendService.respondToFriendRequest(token2, request1.getId(), "accept");

        FriendRequest request2 = friendService.sendFriendRequest(token1, user3.getId());
        friendService.respondToFriendRequest(token3, request2.getId(), "accept");


        List<User> friends = friendService.getFriends(token1);
        assertEquals(2, friends.size());


        boolean foundUser2 = false;
        boolean foundUser3 = false;

        for (User friend : friends) {
            if (friend.getId().equals(user2.getId())) {
                foundUser2 = true;
            }
            else if (friend.getId().equals(user3.getId())) {
                foundUser3 = true;
            }
        }

        assertTrue(foundUser2);
        assertTrue(foundUser3);
    }

    @Test
    public void getAllFriendRequests_Success() {
        friendService.sendFriendRequest(token1, user2.getId());

        friendService.sendFriendRequest(token2, user3.getId());


        friendService.sendFriendRequest(token3, user1.getId());

        List<FriendRequest> allRequests = friendService.getAllFriendRequests(token1);

        assertEquals(2, allRequests.size());
    }

    @Test
    public void bidirectionalFriendshipWithAsymmetricFriendRequests() {
        FriendRequest request1 = friendService.sendFriendRequest(token1, user2.getId());
        friendService.respondToFriendRequest(token2, request1.getId(), "accept");

        List<User> user1Friends = friendService.getFriends(token1);
        List<User> user2Friends = friendService.getFriends(token2);
        assertEquals(1, user1Friends.size());
        assertEquals(1, user2Friends.size());
        assertEquals(user2.getId(), user1Friends.get(0).getId());
        assertEquals(user1.getId(), user2Friends.get(0).getId());
    }
}