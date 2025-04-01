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
        dummyLobby.setLobbyName("Test Lobby");
        dummyLobby.setLobbyCode("ABC12345");
        dummyLobby.setMaxPlayersPerTeam(2);
        dummyLobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE); // Updated to boolean
        // Set mode to match incoming DTO – for example, a team lobby.
        dummyLobby.setMode(LobbyConstants.MODE_TEAM);
        
        dummyLobbyResponseDTO = new LobbyResponseDTO();
        dummyLobbyResponseDTO.setLobbyId(10L);
        dummyLobbyResponseDTO.setLobbyName(dummyLobby.getLobbyName());
        dummyLobbyResponseDTO.setLobbyCode(dummyLobby.getLobbyCode());
        dummyLobbyResponseDTO.setPrivate(dummyLobby.isPrivate()); // Updated to boolean
        // Reflect the mode in the response.
        dummyLobbyResponseDTO.setMode(dummyLobby.getMode());
    }
    
    @Test
    public void testCreateLobby_Valid() throws Exception {
        LobbyRequestDTO requestDTO = new LobbyRequestDTO();
        requestDTO.setLobbyName("Test Lobby");
        requestDTO.setGameType("unranked");
        requestDTO.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE); // Updated to boolean
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
        .andExpect(jsonPath("$.lobbyName").value(dummyLobbyResponseDTO.getLobbyName()))
        .andExpect(jsonPath("$.lobbyCode").value(dummyLobbyResponseDTO.getLobbyCode()))
        .andExpect(jsonPath("$.private").value(dummyLobbyResponseDTO.isPrivate())) // Updated to check boolean
        .andExpect(jsonPath("$.mode").value(dummyLobby.getMode()));
    }
    
    @Test
    public void testUpdateLobbyConfig_Valid() throws Exception {
        LobbyConfigUpdateRequestDTO configDTO = new LobbyConfigUpdateRequestDTO();
        configDTO.setMode(LobbyConstants.MODE_TEAM);
        configDTO.setMaxPlayersPerTeam(2);
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
        joinDTO.setTeam("blue");
        joinDTO.setLobbyCode(dummyLobby.getLobbyCode());
        joinDTO.setFriendInvited(false);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        // Expect the team parameter "blue" for a team join.
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq("blue"), 
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
        joinDTO.setTeam("blue");
        joinDTO.setLobbyCode("WRONG");
        joinDTO.setFriendInvited(false);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq("blue"), 
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
        joinDTO.setTeam("blue");
        joinDTO.setLobbyCode(null);
        joinDTO.setFriendInvited(true);
        
        LobbyJoinResponseDTO joinResponse = new LobbyJoinResponseDTO("Joined lobby successfully.", dummyLobbyResponseDTO);
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.joinLobby(eq(10L), eq(dummyUser.getId()), eq("blue"), 
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
        
        String leaveJson = "{ \"userId\": " + dummyUser.getId() + " }";
        
        mockMvc.perform(
            post("/lobbies/10/leave")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(leaveJson)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Left lobby successfully."));
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
}