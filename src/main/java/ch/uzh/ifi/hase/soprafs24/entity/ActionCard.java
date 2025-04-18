package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "ACTION_CARD")
@Data
public class ActionCard {

    @Id
    private String id;                      // "7choices" or "badsight"

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionCardType type;            // POWERUP or PUNISHMENT

    @Column(nullable = false)
    private String title;                   // e.g. "7 Choices"

    @Column(nullable = false, length = 1000)
    private String description;             // e.g. "Reveal the continent…"

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Explicit getters for MapStruct
    public String getId() {
        return this.id;
    }

    public ActionCardType getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    // expose name for websocket filtering
    public String getName() {
        return this.title;
    }

    // expose owner reference
    public User getOwner() {
        return this.owner;
    }
}