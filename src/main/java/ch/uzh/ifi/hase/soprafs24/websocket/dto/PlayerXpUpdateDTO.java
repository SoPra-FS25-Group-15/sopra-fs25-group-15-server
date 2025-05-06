package ch.uzh.ifi.hase.soprafs24.websocket.dto;

/**
 * DTO for sending XP updates to players
 */
public class PlayerXpUpdateDTO {
    private final String type = "XP_UPDATE";
    private final String username;
    private final Long userId;
    private final int xpGained;
    private final int totalXp;
    private final String reason;

    public PlayerXpUpdateDTO(String username, Long userId, int xpGained, int totalXp, String reason) {
        this.username = username;
        this.userId = userId;
        this.xpGained = xpGained;
        this.totalXp = totalXp;
        this.reason = reason;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public Long getUserId() {
        return userId;
    }

    public int getXpGained() {
        return xpGained;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public String getReason() {
        return reason;
    }
}
