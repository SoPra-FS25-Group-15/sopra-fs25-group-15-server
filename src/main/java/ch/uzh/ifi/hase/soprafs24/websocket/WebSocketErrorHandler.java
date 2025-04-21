package ch.uzh.ifi.hase.soprafs24.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;

@Component
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        // Log detailed error information
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        StompCommand command = accessor != null ? accessor.getCommand() : null;
        String destination = accessor != null ? accessor.getDestination() : null;
        
        logger.error("Error processing client message: command={}, destination={}, error={}",
            command, destination, ex != null ? ex.getMessage() : "Unknown error");
        
        // Log frame details for debugging
        if (accessor != null) {
            accessor.getMessageHeaders().forEach((key, value) -> {
                logger.debug("Header: {} = {}", key, value);
            });
        }
        
        // Handle authentication errors specially
        if (ex != null && (
            (ex.getMessage() != null && ex.getMessage().contains("authentication")) ||
            (ex instanceof MessageDeliveryException && ex.getCause() instanceof org.springframework.web.server.ResponseStatusException))) {
            
            logger.warn("Authentication error detected: {}", ex.getMessage());
            
            // Create error message for client
            StompHeaderAccessor errorHeaderAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
            if (accessor != null) {
                errorHeaderAccessor.setSessionId(accessor.getSessionId());
                errorHeaderAccessor.setReceiptId(accessor.getReceiptId());
            }
            errorHeaderAccessor.setMessage("Authentication failed");
            errorHeaderAccessor.setNativeHeader("status", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        }
        
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }
    
    // Correctly match the signature of the parent class method with proper null checks
    @Override
    protected Message<byte[]> handleInternal(StompHeaderAccessor errorHeaderAccessor, byte[] errorPayload, Throwable cause, StompHeaderAccessor clientHeaderAccessor) {
        // Log error details with null check
        if (cause != null) {
            logger.error("STOMP protocol error: {}", cause.getMessage());
        } else {
            logger.error("STOMP protocol error with null cause");
        }
        
        if (errorHeaderAccessor != null) {
            errorHeaderAccessor.setMessage("An error occurred processing your message");
            if (cause != null && cause.getMessage() != null) {
                errorHeaderAccessor.setNativeHeader("message", cause.getMessage());
            }
        }
        
        // Pass through to default handler
        return super.handleInternal(errorHeaderAccessor, errorPayload, cause, clientHeaderAccessor);
    }
}
