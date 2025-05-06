package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserStatsDTO {
    private int gamesPlayed;
    private int wins;
    private int xp; // Renamed from mmr to xp
    private int points;

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getXp() { // Renamed from getMmr to getXp
        return xp;
    }

    public void setXp(int xp) { // Renamed from setMmr to setXp
        this.xp = xp;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
