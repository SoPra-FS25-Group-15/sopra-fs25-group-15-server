package ch.uzh.ifi.hase.soprafs24.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for token operations.
 * Provides common functionality for token extraction and processing.
 */
public class TokenUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenUtils.class);
    
    /**
     * Extracts the token from an Authorization header
     * Handles "Bearer " prefix and plain tokens
     */
    public static String extractToken(String authHeader) {
        if (authHeader == null) {
            return null;
        }
        
        // If it starts with "Bearer ", remove that prefix
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Otherwise, return the raw token
        return authHeader;
    }
    
    /**
     * Validates if a string looks like a valid token
     */
    public static boolean isValidTokenFormat(String token) {
        // Basic validation - check if it's not null and has reasonable length
        return token != null && token.length() > 8 && token.length() < 150;
    }

    /**
     * Creates a complete authorization header with Bearer prefix if needed
     */
    public static String createAuthorizationHeader(String token) {
        if (token == null) {
            return null;
        }
        
        // If it already starts with "Bearer ", return as is
        if (token.startsWith("Bearer ")) {
            return token;
        }
        
        // Otherwise, add the "Bearer " prefix
        return "Bearer " + token;
    }

    /**
     * Formats a token with Bearer prefix if it doesn't already have one
     * This is effectively an alias for createAuthorizationHeader but with a different name for backwards compatibility
     */
    public static String formatWithBearer(String token) {
        if (token == null) {
            return null;
        }
        
        // If it already starts with "Bearer ", return as is
        if (token.startsWith("Bearer ")) {
            return token;
        }
        
        // Otherwise, add the "Bearer " prefix
        return "Bearer " + token;
    }

    /**
     * Debug helper to log token info without revealing the full token
     */
    public static String maskToken(String token) {
        if (token == null) {
            return "null";
        }
        
        if (token.length() <= 8) {
            return "***" + token.length() + "***";
        }
        
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
