package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.PlayerRoundCard;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("playerRoundCardRepository")
public interface PlayerRoundCardRepository extends JpaRepository<PlayerRoundCard, Long> {
    List<PlayerRoundCard> findByUserAndUsedFalse(User user);
}