package ch.uzh.ifi.hase.soprafs24.entity;


import ch.uzh.ifi.hase.soprafs24.constant.RoundCardType;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "ROUND_CARD")
public class RoundCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoundCardType type;

    @Column
    private String guessType = "Precise"; // Default value

    @Column
    private String streetViewType = "Standard"; // Default value

    @Column
    private Integer roundTimeInSeconds = 60; // Default value

    @Column
    private String mapType = "Standard"; // Default value

    // Standard getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public RoundCardType getType() {
        return type;
    }

    public void setType(RoundCardType type) {
        this.type = type;
    }

    public String getGuessType() {
        return guessType;
    }

    public void setGuessType(String guessType) {
        this.guessType = guessType;
    }

    public String getStreetViewType() {
        return streetViewType;
    }

    public void setStreetViewType(String streetViewType) {
        this.streetViewType = streetViewType;
    }

    public Integer getRoundTimeInSeconds() {
        return roundTimeInSeconds;
    }

    public void setRoundTimeInSeconds(Integer roundTimeInSeconds) {
        this.roundTimeInSeconds = roundTimeInSeconds;
    }

    public String getMapType() {
        return mapType;
    }

    public void setMapType(String mapType) {
        this.mapType = mapType;
    }
}