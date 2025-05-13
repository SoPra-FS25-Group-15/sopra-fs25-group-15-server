package ch.uzh.ifi.hase.soprafs24.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FriendshipTest {

    private Friendship friendship;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        friendship = new Friendship();

        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
    }

    @Test
    void testConstructor_Default() {
        Friendship newFriendship = new Friendship();
        assertNotNull(newFriendship);
        assertNull(newFriendship.getId());
        assertNull(newFriendship.getUser1());
        assertNull(newFriendship.getUser2());
        assertNull(newFriendship.getCreatedAt());
    }

    @Test
    void testUser1() {
        friendship.setUser1(user1);
        assertEquals(user1, friendship.getUser1());
        assertEquals(1L, friendship.getUser1().getId());
    }

    @Test
    void testUser2() {
        friendship.setUser2(user2);
        assertEquals(user2, friendship.getUser2());
        assertEquals(2L, friendship.getUser2().getId());
    }

    @Test
    void testCreatedAt() {
        assertNull(friendship.getCreatedAt());
        // createdAt is set by @PrePersist, tested separately
    }

    @Test
    void testOnCreate() {
        Instant beforeCall = Instant.now();

        // Using reflection to call the protected onCreate method
        try {
            java.lang.reflect.Method onCreateMethod = Friendship.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(friendship);
        } catch (Exception e) {
            fail("Failed to invoke onCreate method: " + e.getMessage());
        }

        Instant afterCall = Instant.now();

        assertNotNull(friendship.getCreatedAt());
        assertTrue(friendship.getCreatedAt().compareTo(beforeCall) >= 0);
        assertTrue(friendship.getCreatedAt().compareTo(afterCall) <= 0);
    }

    @Test
    void testFullyPopulatedFriendship() {
        friendship.setUser1(user1);
        friendship.setUser2(user2);

        // Simulate @PrePersist call
        try {
            java.lang.reflect.Method onCreateMethod = Friendship.class.getDeclaredMethod("onCreate");
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(friendship);
        } catch (Exception e) {
            fail("Failed to invoke onCreate method: " + e.getMessage());
        }

        assertEquals(user1, friendship.getUser1());
        assertEquals(user2, friendship.getUser2());
        assertNotNull(friendship.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        // Test all setters and getters with different values
        User newUser1 = new User();
        newUser1.setId(10L);
        newUser1.setUsername("newUser1");

        User newUser2 = new User();
        newUser2.setId(20L);
        newUser2.setUsername("newUser2");

        friendship.setUser1(newUser1);
        friendship.setUser2(newUser2);

        assertEquals(newUser1, friendship.getUser1());
        assertEquals(newUser2, friendship.getUser2());
        assertEquals(10L, friendship.getUser1().getId());
        assertEquals(20L, friendship.getUser2().getId());
    }

    @Test
    void testNullUsers() {
        friendship.setUser1(null);
        friendship.setUser2(null);

        assertNull(friendship.getUser1());
        assertNull(friendship.getUser2());
    }

    @Test
    void testSwappedUsers() {
        // Test if friendship is valid when users are swapped
        friendship.setUser1(user2);
        friendship.setUser2(user1);

        assertEquals(user2, friendship.getUser1());
        assertEquals(user1, friendship.getUser2());
    }

    @Test
    void testFriendshipBetweenSameUser() {
        // Test edge case where both users are the same
        friendship.setUser1(user1);
        friendship.setUser2(user1);

        assertEquals(user1, friendship.getUser1());
        assertEquals(user1, friendship.getUser2());
        assertSame(friendship.getUser1(), friendship.getUser2());
    }
}