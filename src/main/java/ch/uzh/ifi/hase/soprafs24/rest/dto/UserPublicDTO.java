package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

public class UserPublicDTO {
    private Long userid;
    private String username;
    private int mmr;
    private int points;
    private List<String> achievements;

    // Getters and setters
    public Long getUserid() {
        return userid;
    }
    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
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

    public List<String> getAchievements() {
        return achievements;
    }
    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }
}
