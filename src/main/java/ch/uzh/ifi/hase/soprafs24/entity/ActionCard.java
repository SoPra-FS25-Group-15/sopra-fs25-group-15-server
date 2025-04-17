package ch.uzh.ifi.hase.soprafs24.entity;


import ch.uzh.ifi.hase.soprafs24.constant.ActionCardType;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Internal ActionCard representation
 * This class composes the internal representation of the action cards and defines how they
 * are stored in the database
 */
@Entity
@Table(name = "ACTION_CARD")
@Data
public class ActionCard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionCardType type;

    @Column(nullable = false, length = 1000)
    private String effect;

    @Column(nullable = false)
    private boolean activeFlag;

    @ManyToOne
    private User owner;

    @ManyToOne
    private Game game;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionCard that = (ActionCard) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type);
    }
}