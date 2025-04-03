package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;

@DataJpaTest
public class LobbyRepositoryIntegrationTest {

    @Autowired
    private LobbyRepository lobbyRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void saveLobby_findByLobbyCode_returnsCorrectLobby() {
        // given: create and persist a host user
        User hostUser = new User();
        hostUser.setEmail("host@test.com");
        hostUser.setPassword("password");
        hostUser.setStatus(UserStatus.OFFLINE);
        hostUser = userRepository.save(hostUser);
        
        // given: a new lobby with lobbyType "private" so that a lobby code is generated on persist.
        Lobby lobby = new Lobby();
        lobby.setLobbyName("Integration Test Lobby");
        lobby.setGameType("unranked");
        lobby.setPrivate(true);
        lobby.setMaxPlayersPerTeam(2);
        lobby.setHintsEnabled(List.of("Hint1", "Hint2"));
        lobby.setHost(hostUser);
        
        // when: saving the lobby
        Lobby savedLobby = lobbyRepository.save(lobby);
        
        // then: ID and lobby code are generated
        assertNotNull(savedLobby.getId());
        assertNotNull(savedLobby.getLobbyCode());
    }
    
    @Test
    public void findByLobbyCode_noMatchingLobby_returnsNull() {
        // when: searching with a non-existing code
        Lobby foundLobby = lobbyRepository.findByLobbyCode("NONEXISTENT");
        
        // then: null is returned
        assertNull(foundLobby);
    }
}