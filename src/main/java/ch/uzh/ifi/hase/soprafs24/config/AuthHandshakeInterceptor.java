package ch.uzh.ifi.hase.soprafs24.config;

import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.security.StompPrincipal;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;


public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private final AuthService authService;

    public AuthHandshakeInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Example: Extract token from the "Authorization" header
        String token = request.getHeaders().getFirst("Authorization");
        // Alternatively, you could get the token from query parameters if needed

        if (token != null && !token.isBlank()) {
            try {
                User user = authService.getUserByToken(token);
                // Store a custom principal (using the session token or any unique field)
                attributes.put("user", new StompPrincipal(user.getToken()));
                return true;
            } catch (Exception e) {
                // Token validation failed; reject the handshake
                return false;
            }
        }
        return false; // Reject if token is missing
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No action needed here
    }
}
