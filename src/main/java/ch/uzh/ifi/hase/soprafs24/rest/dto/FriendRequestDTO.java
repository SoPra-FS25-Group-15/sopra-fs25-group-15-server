package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FriendRequestDTO {
    // For sending a friend request, we need the recipient's user id.
    private Long recipient;

    // For responding to a request, we need an action: "accept" or "deny".
    private String action;

    // Optionally, include the request id when returning data.
    private Long requestId;

    // And you might include sender/recipient info in responses.
    // Getters and setters

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
}
