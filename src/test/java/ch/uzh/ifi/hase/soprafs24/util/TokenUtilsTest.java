package ch.uzh.ifi.hase.soprafs24.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TokenUtilsTest {

    @Test
    public void testExtractToken_withBearer() {
        String token = TokenUtils.extractToken("Bearer abc123");
        assertEquals("abc123", token);
    }
    
    @Test
    public void testExtractToken_withoutBearer() {
        String token = TokenUtils.extractToken("abc123");
        assertEquals("abc123", token);
    }
    
    @Test
    public void testExtractToken_null() {
        String token = TokenUtils.extractToken(null);
        assertNull(token);
    }
    
    @Test
    public void testFormatWithBearer_rawToken() {
        String token = TokenUtils.formatWithBearer("abc123");
        assertEquals("Bearer abc123", token);
    }
    
    @Test
    public void testFormatWithBearer_alreadyHasBearer() {
        String token = TokenUtils.formatWithBearer("Bearer abc123");
        assertEquals("Bearer abc123", token);
    }
    
    @Test
    public void testFormatWithBearer_null() {
        String token = TokenUtils.formatWithBearer(null);
        assertNull(token);
    }
}
