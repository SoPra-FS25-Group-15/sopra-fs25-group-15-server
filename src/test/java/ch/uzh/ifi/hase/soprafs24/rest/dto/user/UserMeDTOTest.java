package ch.uzh.ifi.hase.soprafs24.rest.dto.user;


import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMeDTOTest {

    @Test
    void testGetterAndSetter() {
        // Create a new UserMeDTO
        UserMeDTO userMeDTO = new UserMeDTO();

        // Initially all fields should be null
        assertNull(userMeDTO.getUserid());
        assertNull(userMeDTO.getUsername());
        assertNull(userMeDTO.getEmail());
        assertNull(userMeDTO.getToken());

        // Set values
        Long userId = 1L;
        String username = "testUser";
        String email = "test@example.com";
        String token = "test-token-123";

        userMeDTO.setUserid(userId);
        userMeDTO.setUsername(username);
        userMeDTO.setEmail(email);
        userMeDTO.setToken(token);

        // Verify values were set correctly
        assertEquals(userId, userMeDTO.getUserid());
        assertEquals(username, userMeDTO.getUsername());
        assertEquals(email, userMeDTO.getEmail());
        assertEquals(token, userMeDTO.getToken());
    }
}