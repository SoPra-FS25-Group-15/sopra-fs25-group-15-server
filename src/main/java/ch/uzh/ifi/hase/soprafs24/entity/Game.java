package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal Game representation
 * This class composes the internal representation of the game and defines how
 * the game is stored in the database
 */
@Entity
@Table(name = "GAME")
@Data
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String creatorUsername;


    @Column(nullable = false)
    private LocalDateTime creationDate;

    @Column
    private Integer maxPlayers;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_players",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> players = new HashSet<>();


    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ActionCard> actionCards = new HashSet<>();

    @Column
    private Integer currentRound = 0;

    @Column
    private Long currentRoundWinnerId;

    @ManyToOne
    private User winner;
}