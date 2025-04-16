package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.ActionCard;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("actionCardRepository")
public interface ActionCardRepository extends JpaRepository<ActionCard, Long> {

    List<ActionCard> findByOwner(User owner);

    List<ActionCard> findByGame(Game game);

    List<ActionCard> findByOwnerAndGame(User owner, Game game);

    List<ActionCard> findByGameAndActiveFlagTrue(Game game);
}
