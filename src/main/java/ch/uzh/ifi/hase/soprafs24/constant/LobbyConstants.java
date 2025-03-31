package ch.uzh.ifi.hase.soprafs24.constant;

public final class LobbyConstants {
    // Modes
    public static final String MODE_SOLO = "solo";
    public static final String MODE_TEAM = "team";

    // Game types
    public static final String GAME_TYPE_RANKED = "ranked";
    public static final String GAME_TYPE_UNRANKED = "unranked";

    // Lobby types
    // For casual play, only "private" is allowed.
    // For ranked play, lobbyType is ignored (set to null).
    public static final String LOBBY_TYPE_PRIVATE = "private";

    // Lobby statuses
    public static final String LOBBY_STATUS_WAITING = "waiting";
    public static final String LOBBY_STATUS_IN_PROGRESS = "in-progress";

    private LobbyConstants() { }
}


