package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FriendDTO {
    private Long friendId;
    private String username;

    public Long getFriendId() {
        return friendId;
    }
    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
}
