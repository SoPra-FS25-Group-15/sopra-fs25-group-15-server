package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.RoundCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoundCardRepository extends JpaRepository<RoundCard, Long> {
    RoundCard findByName(String name);

    RoundCard findById(long id);
}