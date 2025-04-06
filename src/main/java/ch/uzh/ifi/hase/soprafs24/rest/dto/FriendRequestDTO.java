package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FriendRequestDTO {
    // For sending a friend request, we need the recipient's user id.
    private Long recipient;

    // For responding to a request, we need an action: "accept" or "deny".
    private String action;

    private Long requestId;
    
    // Additional fields for listing requests
    private Long sender;
    private String senderUsername;
    private String recipientUsername;
    private String status;
    private String createdAt;
    private boolean isIncoming; // Whether request was received by current user

    public Long getRecipient() {
        return recipient;
    }
    public void setRecipient(Long recipient) {
        this.recipient = recipient;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public Long getRequestId() {
        return requestId;
    }
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }
    public Long getSender() {
        return sender;
    }
    public void setSender(Long sender) {
        this.sender = sender;
    }
    public String getSenderUsername() {
        return senderUsername;
    }
    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }
    public String getRecipientUsername() {
        return recipientUsername;
    }
    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public boolean isIncoming() {
        return isIncoming;
    }
    public void setIncoming(boolean isIncoming) {
        this.isIncoming = isIncoming;
    }
}
