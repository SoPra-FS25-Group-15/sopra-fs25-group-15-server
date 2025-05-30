package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserPublicDTO {
    private Long userid;
    private String username;
    private int xp; // Renamed from mmr to xp
    private int points;
    private Boolean statsPublic;
    @JsonIgnore
    private List<String> achievements;
    private String email;

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

    public Boolean getStatsPublic() {
        return statsPublic;
    }

    public void setStatsPublic(Boolean statsPublic) {
        this.statsPublic = statsPublic;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
