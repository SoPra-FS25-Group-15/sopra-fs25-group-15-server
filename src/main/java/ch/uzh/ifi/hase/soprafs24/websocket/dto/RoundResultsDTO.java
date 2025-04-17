package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import java.util.List;

public class RoundResultsDTO {
    private boolean gameEnd;
    private List<PlayerResult> results;

    public RoundResultsDTO() {}
    public RoundResultsDTO(boolean gameEnd, List<PlayerResult> results) {
        this.gameEnd = gameEnd;
        this.results = results;
    }

    public boolean isGameEnd() { return gameEnd; }
    public void setGameEnd(boolean gameEnd) { this.gameEnd = gameEnd; }
    public List<PlayerResult> getResults() { return results; }
    public void setResults(List<PlayerResult> results) { this.results = results; }

    public static class PlayerResult {
        private String username;
        private int    score;
        private Integer guess;
        private int    target;

        public PlayerResult() {}
        public PlayerResult(String username, int score, Integer guess, int target) {
            this.username = username;
            this.score    = score;
            this.guess    = guess;
            this.target   = target;
        }

        public String  getUsername() { return username; }
        public void    setUsername(String username) { this.username = username; }
        public int     getScore()    { return score; }
        public void    setScore(int score) { this.score = score; }
        public Integer getGuess()    { return guess; }
        public void    setGuess(Integer guess) { this.guess = guess; }
        public int     getTarget()   { return target; }
        public void    setTarget(int target) { this.target = target; }
    }
}
