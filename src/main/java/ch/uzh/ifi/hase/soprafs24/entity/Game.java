package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * Internal Game representation
 * This class composes the internal representation of the game and defines how
 * the game is stored in the database
 */
@Entity
@Table(name = "GAME")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Game implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private Long currentPlayerId;

    @ManyToOne
    private RoundCard currentRoundCard;

    @Column(nullable = false)
    private String gameStatus;

    @ManyToMany
    private List<User> players;

}

