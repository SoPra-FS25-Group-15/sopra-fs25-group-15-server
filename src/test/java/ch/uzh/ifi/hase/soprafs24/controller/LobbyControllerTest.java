package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        casualResponseDTO.setMaxPlayers("8"); // Changed to String
        casualResponseDTO.setRoundCardsStartAmount(5); // New field
        
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
        .andExpect(jsonPath("$.code").value("12345")) // Updated field name
        .andExpect(jsonPath("$.maxPlayers").value("8")) // Changed to String
        .andExpect(jsonPath("$.roundCardsStartAmount").value(5)) // New field
        .andExpect(jsonPath("$.playersPerTeam").doesNotExist()); // Updated field name
    }
    
    @Test
    public void testUpdateLobbyConfig_Valid() throws Exception {
        LobbyConfigUpdateRequestDTO configDTO = new LobbyConfigUpdateRequestDTO();
        configDTO.setMode(LobbyConstants.MODE_TEAM);
        configDTO.setMaxPlayers(8);
        // Removed roundCards from the request
        
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
        // Set mode to "team" so the controller will pass the team name
        joinDTO.setMode(LobbyConstants.MODE_TEAM);
        // Need to provide the correct lobby code for non-friend joins
        joinDTO.setLobbyCode(dummyLobby.getLobbyCode());
        joinDTO.setFriendInvited(false);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
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
        joinDTO.setLobbyCode("WRONG");  // Wrong code
        joinDTO.setFriendInvited(false); // Not joining as friend
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq(null), 
                eq("WRONG"), eq(false)))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN, "Invalid lobby code"));
        
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
        // For friend invited joins the lobby code is not required
        joinDTO.setMode(LobbyConstants.MODE_TEAM);
        joinDTO.setLobbyCode(null);  // No code needed for friend invites
        joinDTO.setFriendInvited(true);  // Joining as an invited friend
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
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
    
    // Remove or update the testJoinLobby_InvalidLobbyCode test since it's no longer relevant
    // Instead, add a test that confirms both public and private lobbies have codes

    @Test
    public void testCreateLobby_AllLobbiesHaveCodes() throws Exception {
        // Test for public/ranked lobby
        LobbyRequestDTO publicRequestDTO = new LobbyRequestDTO();
        publicRequestDTO.setPrivate(false); // Ranked game (public)
        publicRequestDTO.setMode(LobbyConstants.MODE_TEAM);
        
        Lobby publicLobby = new Lobby();
        publicLobby.setPrivate(false);
        publicLobby.setMode(LobbyConstants.MODE_TEAM);
        publicLobby.setLobbyCode("TEAM123"); // All lobbies get codes now
        
        LobbyResponseDTO publicResponseDTO = new LobbyResponseDTO();
        publicResponseDTO.setLobbyId(10L);
        publicResponseDTO.setLobbyCode("TEAM123");
        publicResponseDTO.setPrivate(false);
        publicResponseDTO.setMode(LobbyConstants.MODE_TEAM);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(mapper.lobbyRequestDTOToEntity(any(LobbyRequestDTO.class))).thenReturn(publicLobby);
        when(lobbyService.createLobby(any(Lobby.class))).thenReturn(publicLobby);
        when(mapper.lobbyEntityToResponseDTO(publicLobby)).thenReturn(publicResponseDTO);
        
        mockMvc.perform(
            post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(publicRequestDTO))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.lobbyCode").value("TEAM123"));
        
        // Test for private/unranked lobby
        LobbyRequestDTO privateRequestDTO = new LobbyRequestDTO();
        privateRequestDTO.setPrivate(true); // Unranked game (private)
        privateRequestDTO.setMode(LobbyConstants.MODE_SOLO);
        
        Lobby privateLobby = new Lobby();
        privateLobby.setPrivate(true);
        privateLobby.setMode(LobbyConstants.MODE_SOLO);
        privateLobby.setLobbyCode("SOLO456"); // All lobbies get codes now
        
        LobbyResponseDTO privateResponseDTO = new LobbyResponseDTO();
        privateResponseDTO.setLobbyId(20L);
        privateResponseDTO.setLobbyCode("SOLO456");
        privateResponseDTO.setPrivate(true);
        privateResponseDTO.setMode(LobbyConstants.MODE_SOLO);
        
        when(mapper.lobbyRequestDTOToEntity(any(LobbyRequestDTO.class))).thenReturn(privateLobby);
        when(lobbyService.createLobby(any(Lobby.class))).thenReturn(privateLobby);
        when(mapper.lobbyEntityToResponseDTO(privateLobby)).thenReturn(privateResponseDTO);
        
        mockMvc.perform(
            post("/lobbies")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(privateRequestDTO))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.lobbyCode").value("SOLO456"));
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

    @Test
    public void testGetAllLobbies_Valid() throws Exception {
        // Create a list of lobbies to be returned by the service
        Lobby lobby1 = new Lobby();
        lobby1.setMode(LobbyConstants.MODE_TEAM);
        lobby1.setPrivate(false);
        lobby1.setLobbyCode("TEAM123");
        
        Lobby lobby2 = new Lobby();
        lobby2.setMode(LobbyConstants.MODE_SOLO);
        lobby2.setPrivate(true);
        lobby2.setLobbyCode("SOLO456");
        
        List<Lobby> lobbies = Arrays.asList(lobby1, lobby2);
        
        // Create corresponding DTOs
        LobbyResponseDTO dto1 = new LobbyResponseDTO();
        dto1.setLobbyId(1L);
        dto1.setMode(LobbyConstants.MODE_TEAM);
        dto1.setPrivate(false);
        dto1.setLobbyCode("TEAM123");
        
        LobbyResponseDTO dto2 = new LobbyResponseDTO();
        dto2.setLobbyId(2L);
        dto2.setMode(LobbyConstants.MODE_SOLO);
        dto2.setPrivate(true);
        dto2.setLobbyCode("SOLO456");
        
        // Setup mocks
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.listLobbies()).thenReturn(lobbies);
        when(mapper.lobbyEntityToResponseDTO(lobby1)).thenReturn(dto1);
        when(mapper.lobbyEntityToResponseDTO(lobby2)).thenReturn(dto2);
        
        // Perform request
        mockMvc.perform(
            get("/lobbies/all_lobbies")
                .header("Authorization", token)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].lobbyId").value(1))
        .andExpect(jsonPath("$[0].mode").value(LobbyConstants.MODE_TEAM))
        .andExpect(jsonPath("$[1].lobbyId").value(2))
        .andExpect(jsonPath("$[1].mode").value(LobbyConstants.MODE_SOLO));
    }
}