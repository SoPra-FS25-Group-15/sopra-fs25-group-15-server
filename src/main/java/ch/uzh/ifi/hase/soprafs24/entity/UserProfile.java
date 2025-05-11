package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "USER_PROFILE")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String username;

    // Renamed from "mmr" to "xp"
    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false)
    private int gamesPlayed;

    @Column(nullable = false)
    private int wins;

    // Flag that determines if others can view your stats
    @Column(nullable = false)
    private boolean statsPublic = true; // default to public

    @ElementCollection
    private List<String> achievements;

    // Example: if you track friend IDs
    @ElementCollection
    private List<Long> friends;

    // Possibly a biography or other fields
    private String biography;

    // Getters and setters
    public Long getId() {
        return id;
    }
    // ...
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

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


    public boolean isStatsPublic() {
        return statsPublic;
    }

    public void setStatsPublic(boolean statsPublic) {
        this.statsPublic = statsPublic;
    }

    // Renamed from getMmr/setMmr to getXp/setXp
    public int getXp() {
        return xp;
    }
    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getPoints() {
        return points;
    }
    public void setPoints(int points) {
        this.points = points;
    }

    public List<String> getAchievements() {
        return achievements;
    }
    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public List<Long> getFriends() {
        return friends;
    }
    public void setFriends(List<Long> friends) {
        this.friends = friends;
    }

    public String getBiography() {
        return biography;
    }
    public void setBiography(String biography) {
        this.biography = biography;
    }
}
