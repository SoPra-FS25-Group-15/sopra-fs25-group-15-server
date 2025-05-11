package ch.uzh.ifi.hase.soprafs24.websocket.dto;

/**
 * DTO for sending XP updates to the client via WebSocket.
 * Used to notify clients when they earn XP in the game.
 */
public class XpUpdateMessage {
    private final int totalXp;
    private final int xpGained;
    private final String reason;

    public XpUpdateMessage(int totalXp, int xpGained, String reason) {
        this.totalXp = totalXp;
        this.xpGained = xpGained;
        this.reason = reason;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getXpGained() {
        return xpGained;
    }

    public String getReason() {
        return reason;
    }
}
