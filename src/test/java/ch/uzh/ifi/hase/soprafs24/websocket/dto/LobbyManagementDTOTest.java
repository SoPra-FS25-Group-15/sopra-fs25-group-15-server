package ch.uzh.ifi.hase.soprafs24.websocket.dto;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LobbyManagementDTOTest {

    @Nested
    class PendingInviteTest {

        @Test
        void testInitialState() {
            LobbyManagementDTO.PendingInvite invite = new LobbyManagementDTO.PendingInvite();
            assertNull(invite.getUsername(), "Username should be null initially.");
            assertNull(invite.getLobbyCode(), "LobbyCode should be null initially.");
        }

        @Test
        void testSetAndGetUsername() {
            LobbyManagementDTO.PendingInvite invite = new LobbyManagementDTO.PendingInvite();
            String username = "InviterUser";
            invite.setUsername(username);
            assertEquals(username, invite.getUsername(), "getUsername should return the set username.");
        }

        @Test
        void testSetAndGetLobbyCode() {
            LobbyManagementDTO.PendingInvite invite = new LobbyManagementDTO.PendingInvite();
            String lobbyCode = "INVITE_LOBBY_789";
            invite.setLobbyCode(lobbyCode);
            assertEquals(lobbyCode, invite.getLobbyCode(), "getLobbyCode should return the set lobbyCode.");
        }

        @Test
        void testSettersWithNullValues() {
            LobbyManagementDTO.PendingInvite invite = new LobbyManagementDTO.PendingInvite();
            invite.setUsername(null);
            assertNull(invite.getUsername(), "Username should be settable to null.");
            invite.setLobbyCode(null);
            assertNull(invite.getLobbyCode(), "LobbyCode should be settable to null.");
        }
    }

    @Nested
    class LobbyManagementDTOMainTest {

        @Test
        void testInitialState() {
            LobbyManagementDTO dto = new LobbyManagementDTO();
            assertNull(dto.getCurrentLobbyCode(), "CurrentLobbyCode should be null initially.");
            assertNull(dto.getPendingInvites(), "PendingInvites list should be null initially.");
        }

        @Test
        void testSetAndGetCurrentLobbyCode() {
            LobbyManagementDTO dto = new LobbyManagementDTO();
            String lobbyCode = "CURRENT_LOBBY_456";
            dto.setCurrentLobbyCode(lobbyCode);
            assertEquals(lobbyCode, dto.getCurrentLobbyCode(), "getCurrentLobbyCode should return the set currentLobbyCode.");
        }

        @Test
        void testSetAndGetPendingInvites() {
            LobbyManagementDTO dto = new LobbyManagementDTO();
            List<LobbyManagementDTO.PendingInvite> invites = new ArrayList<>();
            LobbyManagementDTO.PendingInvite invite1 = new LobbyManagementDTO.PendingInvite();
            invite1.setUsername("UserA");
            invite1.setLobbyCode("LobbyA");
            invites.add(invite1);

            dto.setPendingInvites(invites);
            assertEquals(invites, dto.getPendingInvites(), "getPendingInvites should return the set list of invites.");
            assertNotNull(dto.getPendingInvites());
            assertEquals(1, dto.getPendingInvites().size());
            assertEquals("UserA", dto.getPendingInvites().get(0).getUsername());
        }

        @Test
        void testSettersWithNullValues() {
            LobbyManagementDTO dto = new LobbyManagementDTO();
            dto.setCurrentLobbyCode(null);
            assertNull(dto.getCurrentLobbyCode(), "CurrentLobbyCode should be settable to null.");
            dto.setPendingInvites(null);
            assertNull(dto.getPendingInvites(), "PendingInvites list should be settable to null.");
        }

        @Test
        void testSetPendingInvitesWithEmptyList() {
            LobbyManagementDTO dto = new LobbyManagementDTO();
            List<LobbyManagementDTO.PendingInvite> emptyInvites = new ArrayList<>();
            dto.setPendingInvites(emptyInvites);
            assertNotNull(dto.getPendingInvites(), "PendingInvites list should not be null when set to an empty list.");
            assertTrue(dto.getPendingInvites().isEmpty(), "PendingInvites list should be empty.");
        }
    }
}