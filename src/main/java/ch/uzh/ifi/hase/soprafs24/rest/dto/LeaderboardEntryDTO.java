package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class LeaderboardEntryDTO {
    private String username;
    private int mmr;

    public LeaderboardEntryDTO() {}

    public LeaderboardEntryDTO(String username, int mmr) {
        this.username = username;
        this.mmr = mmr;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public int getMmr() { return mmr; }
    public void setMmr(int mmr) { this.mmr = mmr; }
}
