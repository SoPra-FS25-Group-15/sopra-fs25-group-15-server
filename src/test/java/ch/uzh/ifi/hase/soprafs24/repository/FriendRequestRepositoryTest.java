package ch.uzh.ifi.hase.soprafs24.repository;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;

@DataJpaTest(properties = {
    // turn off the Cloud SQL post-processor
    "spring.cloud.gcp.sql.enabled=false"
})
@AutoConfigureTestDatabase(replace = ANY)
public class FriendRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User sender;
    private User recipient;
    private FriendRequest pendingRequest;
    private FriendRequest acceptedRequest;

    @BeforeEach
    void setUp() {
        // Create users with profiles
        sender = new User();
        sender.setEmail("sender@example.com");
        sender.setPassword("password");
        sender.setStatus(UserStatus.ONLINE);
        
        UserProfile senderProfile = new UserProfile();
        senderProfile.setUsername("Sender");
        sender.setProfile(senderProfile);

        recipient = new User();
        recipient.setEmail("recipient@example.com");
        recipient.setPassword("password");
        recipient.setStatus(UserStatus.ONLINE);
        
        UserProfile recipientProfile = new UserProfile();
        recipientProfile.setUsername("Recipient");
        recipient.setProfile(recipientProfile);

        // Persist users - use the returned managed entities
        sender = entityManager.persist(sender);
        recipient = entityManager.persist(recipient);
        entityManager.flush();

        // Create pending friend request
        pendingRequest = new FriendRequest();
        pendingRequest.setSender(sender);
        pendingRequest.setRecipient(recipient);
        // Let @PrePersist handle setting status to PENDING
        try {
            Field createdAtField = FriendRequest.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(pendingRequest, Instant.now());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set createdAt via reflection", e);
        }
        pendingRequest = entityManager.persist(pendingRequest);
        entityManager.flush();

        // Create accepted friend request (first persist, then update status)
        acceptedRequest = new FriendRequest();
        acceptedRequest.setSender(sender);
        acceptedRequest.setRecipient(recipient);
        try {
            Field createdAtField = FriendRequest.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(acceptedRequest, Instant.now());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set createdAt via reflection", e);
        }
        
        // First persist the entity (this will set status to PENDING via @PrePersist)
        acceptedRequest = entityManager.persist(acceptedRequest);
        entityManager.flush();
        
        // Then update the status to ACCEPTED after it's been persisted
        acceptedRequest.setStatus(FriendRequestStatus.ACCEPTED);
        acceptedRequest = entityManager.merge(acceptedRequest);
        entityManager.flush();
        
        // Verify the status was properly set
        FriendRequest verifyAccepted = entityManager.find(FriendRequest.class, acceptedRequest.getId());
        if (verifyAccepted.getStatus() != FriendRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Failed to set request status to ACCEPTED");
        }
    }

    @Test
    public void findByRecipientAndStatus_ReturnsPendingRequestsOnly() {
        // Verify initial state - check that both requests exist in the database first
        List<FriendRequest> allRequests = friendRequestRepository.findAll();
        assertEquals(2, allRequests.size());
        
        // when: query for pending requests
        List<FriendRequest> foundRequests = friendRequestRepository.findByRecipientAndStatus(recipient, FriendRequestStatus.PENDING);

        // then: verify we found only the pending request
        assertNotNull(foundRequests);
        assertEquals(1, foundRequests.size(), "Should find exactly one pending request");
        assertEquals(recipient.getId(), foundRequests.get(0).getRecipient().getId());
        assertEquals(FriendRequestStatus.PENDING, foundRequests.get(0).getStatus());
    }

    @Test
    public void findBySender_ReturnsAllSentRequests() {
        // when
        List<FriendRequest> foundRequests = friendRequestRepository.findBySender(sender);

        // then
        assertNotNull(foundRequests);
        assertEquals(2, foundRequests.size()); // Should find both pending and accepted requests
        
        // Verify both requests have the correct sender
        assertEquals(sender, foundRequests.get(0).getSender());
        assertEquals(sender, foundRequests.get(1).getSender());
    }

    @Test
    public void findBySenderOrRecipient_ReturnsAllRelatedRequests() {
        // Create a new user
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password");
        anotherUser.setStatus(UserStatus.ONLINE);
        anotherUser = entityManager.persist(anotherUser);

        // Create a request where recipient is the sender to another user
        FriendRequest anotherRequest = new FriendRequest();
        anotherRequest.setSender(recipient);
        anotherRequest.setRecipient(anotherUser);
        anotherRequest.setStatus(FriendRequestStatus.PENDING);
        anotherRequest.onCreate();
        entityManager.persist(anotherRequest);
        entityManager.flush();

        // when: finding all requests where recipient is involved (as sender or recipient)
        List<FriendRequest> foundRequests = friendRequestRepository.findBySenderOrRecipient(recipient, recipient);

        // then: should find the 2 requests where they're the recipient and 1 where they're the sender
        assertNotNull(foundRequests);
        assertEquals(3, foundRequests.size());
        
        // Count the requests where recipient is involved
        long recipientAsRecipientCount = foundRequests.stream()
            .filter(req -> req.getRecipient().equals(recipient))
            .count();
        
        long recipientAsSenderCount = foundRequests.stream()
            .filter(req -> req.getSender().equals(recipient))
            .count();
            
        assertEquals(2, recipientAsRecipientCount); // Two requests where they're the recipient
        assertEquals(1, recipientAsSenderCount);    // One request where they're the sender
    }
}
