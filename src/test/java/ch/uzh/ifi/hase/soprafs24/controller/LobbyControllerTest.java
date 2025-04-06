package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GenericMessageResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.JoinLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyConfigUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyJoinResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyLeaveResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;

@WebMvcTest(LobbyController.class)
public class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private LobbyService lobbyService;
    
    @MockBean
    private DTOMapper mapper;
    
    @MockBean
    private AuthService authService;
    
    private final String token = "dummy-token";
    private User dummyUser;
    private Lobby dummyLobby;
    private LobbyResponseDTO dummyLobbyResponseDTO;
    
    @BeforeEach
    public void setup() throws Exception {
        // Setup a dummy user using reflection to assign an ID
        dummyUser = new User();
        Field userIdField = User.class.getDeclaredField("id");
        userIdField.setAccessible(true);
        userIdField.set(dummyUser, 1L);
        dummyUser.setToken(token);
        
        dummyLobby = new Lobby();
        dummyLobby.setLobbyCode("ABC12345");
        dummyLobby.setMaxPlayersPerTeam(2);
        dummyLobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE); // Updated to boolean
        // Set mode to match incoming DTO – for example, a team lobby.
        dummyLobby.setMode(LobbyConstants.MODE_TEAM);
        
        dummyLobbyResponseDTO = new LobbyResponseDTO();
        dummyLobbyResponseDTO.setLobbyId(10L);
        dummyLobbyResponseDTO.setLobbyCode(dummyLobby.getLobbyCode());
        dummyLobbyResponseDTO.setPrivate(dummyLobby.isPrivate()); // Updated to boolean
        // Reflect the mode in the response.
        dummyLobbyResponseDTO.setMode(dummyLobby.getMode());
    }
    
    @Test
    public void testCreateLobby_Valid() throws Exception {
        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
        requestDTO.setPrivate(true); // Unranked game (private)
        requestDTO.setMaxPlayersPerTeam(2);
        // Send mode field; if omitted the DTO mapper will default to solo.
        requestDTO.setMode(LobbyConstants.MODE_TEAM);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(mapper.lobbyRequestDTOToEntity(any(LobbyRequestDTO.class))).thenReturn(dummyLobby);
        when(lobbyService.createLobby(any(Lobby.class))).thenReturn(dummyLobby);
        when(mapper.lobbyEntityToResponseDTO(dummyLobby)).thenReturn(dummyLobbyResponseDTO);
        
        mockMvc.perform(
            post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.lobbyId").value(dummyLobbyResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.lobbyCode").value(dummyLobbyResponseDTO.getLobbyCode()))
        .andExpect(jsonPath("$.private").value(dummyLobbyResponseDTO.isPrivate()))
        .andExpect(jsonPath("$.mode").value(dummyLobby.getMode()));
    }

    @Test
    public void testCreateCasualLobby_ForcesSoloMode() throws Exception {
        // Create a request for a casual lobby, trying to set team mode
        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
        requestDTO.setPrivate(true); // Unranked (private)
        requestDTO.setMode("team"); // This should be overridden to solo
        requestDTO.setMaxPlayersPerTeam(2); // This should be ignored
        
        // Configure the Lobby that would be created (with solo mode enforced)
        Lobby casualLobby = new Lobby();
        casualLobby.setPrivate(true); // Private = unranked
        casualLobby.setMode("solo"); // Solo mode is enforced
        casualLobby.setMaxPlayersPerTeam(1); // Always 1 for solo
        casualLobby.setLobbyCode("12345"); // Generated code
        
        LobbyResponseDTO casualResponseDTO = new LobbyResponseDTO();
        casualResponseDTO.setLobbyId(20L);
        casualResponseDTO.setMode("solo");
        casualResponseDTO.setPrivate(true);
        casualResponseDTO.setLobbyCode("12345");
        casualResponseDTO.setMaxPlayers(8); // For solo mode, this is set
        // maxPlayersPerTeam should not appear in the response for solo mode
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(mapper.lobbyRequestDTOToEntity(any(LobbyRequestDTO.class))).thenReturn(casualLobby);
        when(lobbyService.createLobby(any(Lobby.class))).thenReturn(casualLobby);
        when(mapper.lobbyEntityToResponseDTO(casualLobby)).thenReturn(casualResponseDTO);
        
        // Execute request and verify the response
        mockMvc.perform(
            post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.lobbyId").value(casualResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.mode").value("solo")) // Mode is enforced to solo
        .andExpect(jsonPath("$.lobbyCode").value("12345")) // Has generated code
        .andExpect(jsonPath("$.maxPlayers").value(8)) // MaxPlayers is shown
        .andExpect(jsonPath("$.maxPlayersPerTeam").doesNotExist()); // No maxPlayersPerTeam in solo mode
    }
    
    @Test
    public void testUpdateLobbyConfig_Valid() throws Exception {
        LobbyConfigUpdateRequestDTO configDTO = new LobbyConfigUpdateRequestDTO();
        configDTO.setMode(LobbyConstants.MODE_TEAM);
        configDTO.setMaxPlayers(8);
        configDTO.setRoundCards(Arrays.asList("Card1", "Card2"));
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.updateLobbyConfig(eq(10L), any(LobbyConfigUpdateRequestDTO.class), eq(dummyUser.getId())))
            .thenReturn(dummyLobby);
        when(mapper.lobbyEntityToResponseDTO(dummyLobby)).thenReturn(dummyLobbyResponseDTO);
        
        mockMvc.perform(
            put("/lobbies/10/config")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(configDTO))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lobbyId").value(dummyLobbyResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.mode").value(dummyLobby.getMode()));
    }
    
    @Test
    public void testJoinLobby_Valid() throws Exception {
        JoinLobbyRequestDTO joinDTO = new JoinLobbyRequestDTO();
        // Set mode to "team" so the controller will pass the team name.
        joinDTO.setMode(LobbyConstants.MODE_TEAM);
        joinDTO.setLobbyCode(dummyLobby.getLobbyCode());
        joinDTO.setFriendInvited(false);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        // No team parameter is passed anymore
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq(null), 
                eq(dummyLobby.getLobbyCode()), eq(false)))
            .thenReturn(joinResponse);
        
        mockMvc.perform(
            post("/lobbies/10/join?userId=" + dummyUser.getId())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinDTO))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Joined lobby successfully."))
        .andExpect(jsonPath("$.lobby.lobbyId").value(dummyLobbyResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.lobby.mode").value(dummyLobby.getMode()));
    }
    
    @Test
    public void testJoinLobby_InvalidLobbyCode() throws Exception {
        JoinLobbyRequestDTO joinDTO = new JoinLobbyRequestDTO();
        joinDTO.setMode(LobbyConstants.MODE_TEAM);
        joinDTO.setLobbyCode("WRONG");
        joinDTO.setFriendInvited(false);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq(null), 
                eq("WRONG"), eq(false)))
            .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Invalid lobby code"));
        
        mockMvc.perform(
            post("/lobbies/10/join?userId=" + dummyUser.getId())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinDTO))
        )
        .andExpect(status().isForbidden());
    }
    
    @Test
    public void testJoinLobby_FriendInvited_Valid() throws Exception {
        JoinLobbyRequestDTO joinDTO = new JoinLobbyRequestDTO();
        // For friend invited joins the lobby code is not required.
        joinDTO.setMode(LobbyConstants.MODE_TEAM);
        joinDTO.setLobbyCode(null);
        joinDTO.setFriendInvited(true);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        // No team parameter is passed anymore
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq(null), 
                eq(null), eq(true)))
            .thenReturn(joinResponse);
        
        mockMvc.perform(
            post("/lobbies/10/join?userId=" + dummyUser.getId())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinDTO))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Joined lobby successfully."))
        .andExpect(jsonPath("$.lobby.lobbyId").value(dummyLobbyResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.lobby.mode").value(dummyLobby.getMode()));
    }
    
    @Test
    public void testLeaveLobby_Valid() throws Exception {
        LobbyLeaveResponseDTO leaveResponse = new LobbyLeaveResponseDTO("Left lobby successfully.", dummyLobbyResponseDTO);
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.leaveLobby(eq(10L), eq(dummyUser.getId()), eq(dummyUser.getId())))
            .thenReturn(leaveResponse);
        
        mockMvc.perform(
            delete("/lobbies/10/leave")
                .header("Authorization", token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Left lobby successfully."));
    }
    
    @Test
    public void testKickFromLobby_Valid() throws Exception {
        Long kickedUserId = 2L;
        LobbyLeaveResponseDTO leaveResponse = new LobbyLeaveResponseDTO("User kicked from lobby successfully.", dummyLobbyResponseDTO);
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.leaveLobby(eq(10L), eq(dummyUser.getId()), eq(kickedUserId)))
            .thenReturn(leaveResponse);
        
        mockMvc.perform(
            delete("/lobbies/10/leave?userId=" + kickedUserId)
                .header("Authorization", token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("User kicked from lobby successfully."));
    }
    
    @Test
    public void testDeleteLobby_Valid() throws Exception {
        GenericMessageResponseDTO deleteResponse = new GenericMessageResponseDTO("Lobby disbanded successfully.");
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.deleteLobby(eq(10L), eq(dummyUser.getId())))
            .thenReturn(deleteResponse);
        
        mockMvc.perform(
            delete("/lobbies/10")
                .header("Authorization", token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Lobby disbanded successfully."));
    }

    @Test
    public void testJoinCasualLobby_AsFriend() throws Exception {
        // For casual (solo mode) lobbies, joining as a friend
        JoinLobbyRequestDTO joinDTO = new JoinLobbyRequestDTO();
        joinDTO.setMode("solo");
        // Removed team field setting since it was removed from the DTO
        joinDTO.setLobbyCode(null); // No code needed when invited as friend
        joinDTO.setFriendInvited(true);
        
        // Configure response
        LobbyResponseDTO soloResponseDTO = new LobbyResponseDTO();
        soloResponseDTO.setLobbyId(20L);
        soloResponseDTO.setMode("solo");
        soloResponseDTO.setMaxPlayers(8);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", soloResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.joinLobby(eq(20L), eq(dummyUser.getId()), eq(null), 
                eq(null), eq(true))) // Friend invite doesn't require code
            .thenReturn(joinResponse);
        
        mockMvc.perform(
            post("/lobbies/20/join?userId=" + dummyUser.getId())
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(joinDTO))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Joined lobby successfully."))
        .andExpect(jsonPath("$.lobby.lobbyId").value(soloResponseDTO.getLobbyId()))
        .andExpect(jsonPath("$.lobby.mode").value("solo"));
    }
}