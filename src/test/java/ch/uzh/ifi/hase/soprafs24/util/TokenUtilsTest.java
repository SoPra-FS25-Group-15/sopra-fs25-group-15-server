package ch.uzh.ifi.hase.soprafs24.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenUtilsTest {

    @Test
    void extractToken_withBearerPrefix_returnsToken() {

        String authHeader = "Bearer test-token";


        String result = TokenUtils.extractToken(authHeader);


        assertEquals("test-token", result);
    }

    @Test
    void extractToken_withoutPrefix_returnsOriginal() {

        String plainToken = "test-token";


        String result = TokenUtils.extractToken(plainToken);


        assertEquals(plainToken, result);
    }

    @Test
    void extractToken_withNull_returnsNull() {

        String result = TokenUtils.extractToken(null);


        assertNull(result);
    }

    @Test
    void formatWithBearer_addsPrefix() {

        String token = "test-token";


        String result = TokenUtils.formatWithBearer(token);


        assertEquals("Bearer test-token", result);
    }

    @Test
    void formatWithBearer_withExistingPrefix_returnsSame() {

        String token = "Bearer test-token";


        String result = TokenUtils.formatWithBearer(token);


        assertEquals(token, result);
    }

    @Test
    void formatWithBearer_withNull_returnsNull() {

        String result = TokenUtils.formatWithBearer(null);


        assertNull(result);
    }

    @Test
    void maskToken_hidesMiddlePortion() {

        String token = "abcdefghijklm";


        String result = TokenUtils.maskToken(token);


        assertEquals("abcd...jklm", result);
    }

    @Test
    void maskToken_withShortToken_returnsLengthIndicator() {

        String token = "short";


        String result = TokenUtils.maskToken(token);


        assertEquals("***5***", result);
    }

    @Test
    void maskToken_withNull_returnsNullString() {

        String result = TokenUtils.maskToken(null);


        assertEquals("null", result);
    }

    @Test
    void isValidTokenFormat_withValidToken_returnsTrue() {

        String token = "valid-token-with-sufficient-length";


        boolean result = TokenUtils.isValidTokenFormat(token);


        assertTrue(result);
    }

    @Test
    void isValidTokenFormat_withShortToken_returnsFalse() {

        String token = "short";


        boolean result = TokenUtils.isValidTokenFormat(token);


        assertFalse(result);
    }

    @Test
    void isValidTokenFormat_withNullToken_returnsFalse() {

        boolean result = TokenUtils.isValidTokenFormat(null);


        assertFalse(result);
    }

    @Test
    void isValidTokenFormat_withTooLongToken_returnsFalse() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 160; i++) {
            sb.append("a");
        }
        String token = sb.toString();


        boolean result = TokenUtils.isValidTokenFormat(token);


        assertFalse(result);
    }

    @Test
    void createAuthorizationHeader_addsPrefix() {

        String token = "test-token";


        String result = TokenUtils.createAuthorizationHeader(token);


        assertEquals("Bearer test-token", result);
    }

    @Test
    void createAuthorizationHeader_withExistingPrefix_returnsSame() {

        String token = "Bearer test-token";


        String result = TokenUtils.createAuthorizationHeader(token);


        assertEquals(token, result);
    }

    @Test
    void createAuthorizationHeader_withNull_returnsNull() {

        String result = TokenUtils.createAuthorizationHeader(null);


        assertNull(result);
    }
}