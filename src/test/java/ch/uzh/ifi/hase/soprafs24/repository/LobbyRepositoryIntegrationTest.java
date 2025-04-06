package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        
        // given: a new private lobby (PrePersist will generate a code)
        Lobby privateLobby = new Lobby();
        privateLobby.setPrivate(true); // Private = unranked game
        privateLobby.setMaxPlayersPerTeam(1); // Solo mode
        privateLobby.setMode("solo");
        privateLobby.setHintsEnabled(List.of("Hint1", "Hint2"));
        privateLobby.setHost(hostUser);
        // Do NOT set a lobby code manually for private lobby
        privateLobby = lobbyRepository.saveAndFlush(privateLobby);
        
        // Retrieve the generated lobby code from the saved entity
        String generatedCode = privateLobby.getLobbyCode();
        assertNotNull(generatedCode);
        
        // when: finding the private lobby by its generated code
        Lobby foundPrivateLobby = lobbyRepository.findByLobbyCode(generatedCode);
        if (foundPrivateLobby == null) {
            throw new AssertionError("Private lobby not found");
        }
        // then: check lobby code is preserved (matches the generated value)
        assertNotNull(foundPrivateLobby.getId());
        assertEquals(generatedCode, foundPrivateLobby.getLobbyCode());
        
        // given: a new public lobby (PrePersist does not override manually set code)
        Lobby publicLobby = new Lobby();
        publicLobby.setPrivate(false); // Public = ranked game
        publicLobby.setMaxPlayersPerTeam(2); // Team mode
        publicLobby.setMode("team");
        publicLobby.setHintsEnabled(List.of("Hint1", "Hint2"));
        publicLobby.setHost(hostUser);
        // Set a lobby code manually
        String testCodePublic = "TEST456";
        publicLobby.setLobbyCode(testCodePublic);
        publicLobby = lobbyRepository.saveAndFlush(publicLobby);
        
        // when: finding the public lobby by code
        Lobby foundPublicLobby = lobbyRepository.findByLobbyCode(testCodePublic);
        if (foundPublicLobby == null) {
            throw new AssertionError("Public lobby not found");
        }
        // then: check lobby code is preserved
        assertNotNull(foundPublicLobby.getId());
        assertEquals(testCodePublic, foundPublicLobby.getLobbyCode());
    }
    
    @Test
    public void findByLobbyCode_noMatchingLobby_returnsNull() {
        // when: searching with a non-existing code
        Lobby foundLobby = lobbyRepository.findByLobbyCode("NONEXISTENT");
        
        // then: null is returned
        assertNull(foundLobby);
    }
}