package ch.uzh.ifi.hase.soprafs24.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    @Test
    void extractToken_withBearerPrefix_returnsToken() {
        // given
        String authHeader = "Bearer test-token";
        
        // when
        String result = TokenUtils.extractToken(authHeader);
        
        // then
        assertEquals("test-token", result);
    }

    @Test
    void extractToken_withoutPrefix_returnsOriginal() {
        // given
        String plainToken = "test-token";
        
        // when
        String result = TokenUtils.extractToken(plainToken);
        
        // then
        assertEquals(plainToken, result);
    }

    @Test
    void extractToken_withNull_returnsNull() {
        // when
        String result = TokenUtils.extractToken(null);
        
        // then
        assertNull(result);
    }

    @Test
    void formatWithBearer_addsPrefix() {
        // given
        String token = "test-token";
        
        // when
        String result = TokenUtils.formatWithBearer(token);
        
        // then
        assertEquals("Bearer test-token", result);
    }

    @Test
    void formatWithBearer_withExistingPrefix_returnsSame() {
        // given
        String token = "Bearer test-token";
        
        // when
        String result = TokenUtils.formatWithBearer(token);
        
        // then
        assertEquals(token, result);
    }

    @Test
    void maskToken_hidesMiddlePortion() {
        // given
        String token = "abcdefghijklm";
        
        // when
        String result = TokenUtils.maskToken(token);
        
        // then
        assertEquals("abcd...jklm", result);
    }
}
