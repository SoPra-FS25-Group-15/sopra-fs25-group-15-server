package ch.uzh.ifi.hase.soprafs24.rest.dto.friend;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FriendRequestDTOTest {

    @Test
    public void testFriendRequestDTO_Creation() {
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        assertNotNull(friendRequestDTO);
    }

    @Test
    public void testFriendRequestDTO_GetAndSetRecipient() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        Long recipient = 1L;

        // Exercise
        friendRequestDTO.setRecipient(recipient);

        // Verify
        assertEquals(recipient, friendRequestDTO.getRecipient());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetAction() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String action = "accept";

        // Exercise
        friendRequestDTO.setAction(action);

        // Verify
        assertEquals(action, friendRequestDTO.getAction());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetRequestId() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        Long requestId = 123L;

        // Exercise
        friendRequestDTO.setRequestId(requestId);

        // Verify
        assertEquals(requestId, friendRequestDTO.getRequestId());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetSender() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        Long sender = 456L;

        // Exercise
        friendRequestDTO.setSender(sender);

        // Verify
        assertEquals(sender, friendRequestDTO.getSender());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetSenderUsername() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String senderUsername = "testSender";

        // Exercise
        friendRequestDTO.setSenderUsername(senderUsername);

        // Verify
        assertEquals(senderUsername, friendRequestDTO.getSenderUsername());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetRecipientUsername() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String recipientUsername = "testRecipient";

        // Exercise
        friendRequestDTO.setRecipientUsername(recipientUsername);

        // Verify
        assertEquals(recipientUsername, friendRequestDTO.getRecipientUsername());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetStatus() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String status = "pending";

        // Exercise
        friendRequestDTO.setStatus(status);

        // Verify
        assertEquals(status, friendRequestDTO.getStatus());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetCreatedAt() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String createdAt = "2024-04-22T10:30:00";

        // Exercise
        friendRequestDTO.setCreatedAt(createdAt);

        // Verify
        assertEquals(createdAt, friendRequestDTO.getCreatedAt());
    }

    @Test
    public void testFriendRequestDTO_GetAndSetIncoming() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        boolean isIncoming = true;

        // Exercise
        friendRequestDTO.setIncoming(isIncoming);

        // Verify
        assertTrue(friendRequestDTO.isIncoming());

        // Test opposite case
        friendRequestDTO.setIncoming(false);
        assertFalse(friendRequestDTO.isIncoming());
    }

    @Test
    public void testFriendRequestDTO_AllPropertiesSet() {
        // Setup
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        Long recipient = 1L;
        String action = "accept";
        Long requestId = 123L;
        Long sender = 456L;
        String senderUsername = "testSender";
        String recipientUsername = "testRecipient";
        String status = "pending";
        String createdAt = "2024-04-22T10:30:00";
        boolean isIncoming = true;

        // Exercise
        friendRequestDTO.setRecipient(recipient);
        friendRequestDTO.setAction(action);
        friendRequestDTO.setRequestId(requestId);
        friendRequestDTO.setSender(sender);
        friendRequestDTO.setSenderUsername(senderUsername);
        friendRequestDTO.setRecipientUsername(recipientUsername);
        friendRequestDTO.setStatus(status);
        friendRequestDTO.setCreatedAt(createdAt);
        friendRequestDTO.setIncoming(isIncoming);

        // Verify
        assertEquals(recipient, friendRequestDTO.getRecipient());
        assertEquals(action, friendRequestDTO.getAction());
        assertEquals(requestId, friendRequestDTO.getRequestId());
        assertEquals(sender, friendRequestDTO.getSender());
        assertEquals(senderUsername, friendRequestDTO.getSenderUsername());
        assertEquals(recipientUsername, friendRequestDTO.getRecipientUsername());
        assertEquals(status, friendRequestDTO.getStatus());
        assertEquals(createdAt, friendRequestDTO.getCreatedAt());
        assertTrue(friendRequestDTO.isIncoming());
    }

    @Test
    public void testFriendRequestDTO_AcceptAction() {
        // Setup - testing with a specific action value
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String action = "accept";

        // Exercise
        friendRequestDTO.setAction(action);

        // Verify
        assertEquals(action, friendRequestDTO.getAction());
    }

    @Test
    public void testFriendRequestDTO_DenyAction() {
        // Setup - testing with another specific action value
        FriendRequestDTO friendRequestDTO = new FriendRequestDTO();
        String action = "deny";

        // Exercise
        friendRequestDTO.setAction(action);

        // Verify
        assertEquals(action, friendRequestDTO.getAction());
    }
}