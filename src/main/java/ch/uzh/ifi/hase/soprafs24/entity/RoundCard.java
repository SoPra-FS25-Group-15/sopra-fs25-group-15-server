package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO.RoundCardModifiers;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Round Card Entity representing a round card in the game
 */
@Entity
@Table(name = "ROUND_CARD")
public class RoundCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Embedded
    private RoundCardModifiers modifiers;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public RoundCardModifiers getModifiers() {
        return modifiers;
    }

    public void setModifiers(RoundCardModifiers modifiers) {
        this.modifiers = modifiers;
    }
}
