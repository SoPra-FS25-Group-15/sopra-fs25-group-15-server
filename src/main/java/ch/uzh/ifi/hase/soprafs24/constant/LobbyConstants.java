package ch.uzh.ifi.hase.soprafs24.constant;

public final class LobbyConstants {
    // Modes
    public static final String MODE_SOLO = "solo";
    public static final String MODE_TEAM = "team";

    // Lobby types - isPrivate flag determines the lobby type
    // false = ranked (public lobbies)
    // true = unranked (private lobbies)
    public static final boolean IS_LOBBY_PRIVATE = true;

    // Lobby statuses
    public static final String LOBBY_STATUS_WAITING = "waiting";
    public static final String LOBBY_STATUS_IN_PROGRESS = "in-progress";

    private LobbyConstants() { }
}


