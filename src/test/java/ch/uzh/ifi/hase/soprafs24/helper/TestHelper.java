package ch.uzh.ifi.hase.soprafs24.helper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

import java.lang.reflect.Field;

/**
 * Helper methods for tests to avoid repetitive code
 */
public class TestHelper {
    
    /**
     * Set ID on an entity using reflection
     */
    public static <T> void setId(T entity, Long id, String fieldName) {
        try {
            Field idField = entity.getClass().getDeclaredField(fieldName);
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set " + fieldName + " via reflection", e);
        }
    }
    
    /**
     * Set ID on User entity using reflection
     */
    public static void setUserId(User user, Long id) {
        setId(user, id, "id");
    }
    
    /**
     * Set ID on Lobby entity using reflection
     */
    public static void setLobbyId(Lobby lobby, Long id) {
        setId(lobby, id, "id");
    }
}
