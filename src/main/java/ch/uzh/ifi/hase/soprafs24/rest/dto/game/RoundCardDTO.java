package ch.uzh.ifi.hase.soprafs24.rest.dto.game;

import java.io.Serializable;

/**
 * Data Transfer Object for Round Cards
 * Contains all information about a round card including modifiers
 */
public class RoundCardDTO implements Serializable {

    private String id;
    private String name;
    private String description;
    private RoundCardModifiers modifiers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoundCardModifiers getModifiers() {
        return modifiers;
    }

    public void setModifiers(RoundCardModifiers modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * Inner class representing round card modifiers
     */
    public static class RoundCardModifiers implements Serializable {
        private String guessType;
        private String streetView;
        private int time;  // Round time in seconds

        public String getGuessType() {
            return guessType;
        }

        public void setGuessType(String guessType) {
            this.guessType = guessType;
        }

        public String getStreetView() {
            return streetView;
        }

        public void setStreetView(String streetView) {
            this.streetView = streetView;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }
    }
}
