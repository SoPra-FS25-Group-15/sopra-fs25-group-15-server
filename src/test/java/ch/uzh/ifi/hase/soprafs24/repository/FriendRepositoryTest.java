package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class FriendRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @BeforeEach
    void setUp() {
        // Create and persist sender and recipient users
        User sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPassword("password");
        sender.setStatus(UserStatus.ONLINE);

        User recipient = new User();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);

        entityManager.persist(sender);
        entityManager.persist(recipient);
        entityManager.flush();

        // Create and persist a friend request
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setRecipient(recipient);
        request.setStatus(FriendRequestStatus.PENDING);
        request.onCreate();

        entityManager.persist(request);
        entityManager.flush();
    }

    @Test
    public void findByRecipientAndStatus_success() {
        // given
        User recipient = entityManager.find(User.class, 2L); // Assuming recipient has ID 2
        FriendRequestStatus status = FriendRequestStatus.PENDING;

        // when
        List<FriendRequest> foundRequests = friendRequestRepository.findByRecipientAndStatus(recipient, status);

        // then
        assertNotNull(foundRequests);
        assertEquals(1, foundRequests.size());
        assertEquals(recipient, foundRequests.get(0).getRecipient());
        assertEquals(status, foundRequests.get(0).getStatus());
    }
}