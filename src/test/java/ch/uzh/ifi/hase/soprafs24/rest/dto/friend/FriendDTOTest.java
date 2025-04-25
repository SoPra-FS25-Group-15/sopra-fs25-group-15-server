package ch.uzh.ifi.hase.soprafs24.rest.dto.friend;

import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class FriendDTOTest {

    @Test
    public void testFriendDTO_Creation() {
        FriendDTO friendDTO = new FriendDTO();
        assertNotNull(friendDTO);
    }

    @Test
    public void testFriendDTO_GetAndSetFriendId() {
        // Setup
        FriendDTO friendDTO = new FriendDTO();
        Long friendId = 1L;

        // Exercise
        friendDTO.setFriendId(friendId);

        // Verify
        assertEquals(friendId, friendDTO.getFriendId());
    }

    @Test
    public void testFriendDTO_GetAndSetUsername() {
        // Setup
        FriendDTO friendDTO = new FriendDTO();
        String username = "testUser";

        // Exercise
        friendDTO.setUsername(username);

        // Verify
        assertEquals(username, friendDTO.getUsername());
    }

    @Test
    public void testFriendDTO_AllPropertiesSet() {
        // Setup
        FriendDTO friendDTO = new FriendDTO();
        Long friendId = 1L;
        String username = "testUser";

        // Exercise
        friendDTO.setFriendId(friendId);
        friendDTO.setUsername(username);

        // Verify
        assertEquals(friendId, friendDTO.getFriendId());
        assertEquals(username, friendDTO.getUsername());
    }
}