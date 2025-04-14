package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("gameRepository")
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findById(Long id);

    Game findByName(String name);

    List<Game> findByStatus(GameStatus status);

    @Query("SELECT g FROM Game g JOIN g.players p WHERE p = :player")
    List<Game> findByPlayer(@Param("player") User player);
}
