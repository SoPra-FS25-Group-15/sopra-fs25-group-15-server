package ch.uzh.ifi.hase.soprafs24.rest.dto;
import java.util.List;
import java.util.stream.Collectors;

public class UserStatsDTO {
    private int gamesPlayed;
    private int wins;
    private int mmr;
    private int winStreak;
    private List<String> lastGamePlayers;
    private String lastGameWinner;

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

    public int getMmr() {
        return mmr;
    }

    public void setMmr(int mmr) {
        this.mmr = mmr;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getWinStreak() {
        return winStreak;
    }
    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }
    
    public List<String> getLastGamePlayers() {
        return lastGamePlayers;
    }
    public void setLastGamePlayers(List<String> lastGamePlayers) {
        this.lastGamePlayers = lastGamePlayers;
    }
    
    public String getLastGameWinner() {
        return lastGameWinner;
    }
    public void setLastGameWinner(String lastGameWinner) {
        this.lastGameWinner = lastGameWinner;
    }
}
