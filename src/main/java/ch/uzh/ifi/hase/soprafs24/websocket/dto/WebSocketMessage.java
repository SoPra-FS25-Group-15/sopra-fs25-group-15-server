package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.time.Instant;

public class WebSocketMessage<T> {
    
    private String type;
    private T payload;
    private Instant timestamp;
    
    // Default constructor for JSON deserialization
    public WebSocketMessage() {
        this.timestamp = Instant.now();
    }
    
    // Constructor with type and payload
    public WebSocketMessage(String type, T payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now();
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public T getPayload() {
        return payload;
    }
    
    public void setPayload(T payload) {
        this.payload = payload;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
