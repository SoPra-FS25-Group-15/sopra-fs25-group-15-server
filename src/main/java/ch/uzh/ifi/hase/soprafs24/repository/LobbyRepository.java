package ch.uzh.ifi.hase.soprafs24.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.Lobby;

@Repository
public interface LobbyRepository extends JpaRepository<Lobby, Long> {
    Lobby findByLobbyCode(String lobbyCode);
}
