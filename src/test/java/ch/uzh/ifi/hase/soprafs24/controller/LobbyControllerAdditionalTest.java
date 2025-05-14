package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs24.constant.LobbyConstants;
import ch.uzh.ifi.hase.soprafs24.entity.Lobby;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InviteLobbyRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyInviteResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LobbyResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.LobbyService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "spring.cloud.gcp.sql.enabled=false")
@WebMvcTest(LobbyController.class)
public class LobbyControllerAdditionalTest {

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
    
    private final String token = "Bearer dummy-token";
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
        dummyLobby.setPrivate(LobbyConstants.IS_LOBBY_PRIVATE);
        dummyLobby.setMode(LobbyConstants.MODE_TEAM);
        
        dummyLobbyResponseDTO = new LobbyResponseDTO();
        dummyLobbyResponseDTO.setLobbyId(10L);
        dummyLobbyResponseDTO.setLobbyCode(dummyLobby.getLobbyCode());
        dummyLobbyResponseDTO.setPrivate(dummyLobby.isPrivate());
        dummyLobbyResponseDTO.setMode(dummyLobby.getMode());
        dummyLobbyResponseDTO.setMaxPlayers(4);
    }
    
    @Test
    public void getLobby_ValidId_ReturnsLobbyDetails() throws Exception {
        // given
        when(lobbyService.getLobbyById(eq(10L))).thenReturn(dummyLobby);
        when(mapper.lobbyEntityToResponseDTO(dummyLobby)).thenReturn(dummyLobbyResponseDTO);
        
        // when/then
        mockMvc.perform(
            get("/lobbies/10")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lobbyId").value(dummyLobbyResponseDTO.getLobbyId()))
            .andExpect(jsonPath("$.lobbyCode").value(dummyLobbyResponseDTO.getLobbyCode()))
            .andExpect(jsonPath("$.private").value(dummyLobbyResponseDTO.isPrivate()))
            .andExpect(jsonPath("$.mode").value(dummyLobbyResponseDTO.getMode()))
            .andExpect(jsonPath("$.maxPlayers").value(4));
    }
    
    @Test
    public void getLobby_InvalidId_ReturnsNotFound() throws Exception {
        when(lobbyService.getLobbyById(eq(99L)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found"));

        mockMvc.perform(
            get("/lobbies/99")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    @Test
    public void inviteToLobby_ValidRequest_ReturnsInviteResponse() throws Exception {
        // given
        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(2L);
        
        LobbyInviteResponseDTO inviteResponseDTO = new LobbyInviteResponseDTO();
        inviteResponseDTO.setInvitedFriend("friendUsername");
        inviteResponseDTO.setLobbyCode(dummyLobby.getLobbyCode());
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.inviteToLobby(eq(10L), eq(dummyUser.getId()), any(InviteLobbyRequestDTO.class)))
            .thenReturn(inviteResponseDTO);
        
        mockMvc.perform(
            post("/lobbies/10/invite")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invitedFriend").value("friendUsername"))
            .andExpect(jsonPath("$.lobbyCode").value(dummyLobby.getLobbyCode()));
    }
    
    @Test
    public void inviteToLobby_MissingAuthHeader_ReturnsBadRequest() throws Exception {
        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(2L);

        mockMvc.perform(
            post("/lobbies/10/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    public void inviteToLobby_InvalidToken_ReturnsUnauthorized() throws Exception {
        String badToken = "Bearer invalid-token";
        when(authService.getUserByToken(eq(badToken)))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token"));

        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(2L);

        mockMvc.perform(
            post("/lobbies/10/invite")
                .header("Authorization", badToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    public void inviteToLobby_NonHost_ReturnsForbidden() throws Exception {
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.inviteToLobby(eq(10L), eq(dummyUser.getId()), any(InviteLobbyRequestDTO.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the host can invite players"));

        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(2L);

        mockMvc.perform(
            post("/lobbies/10/invite")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isForbidden());
    }
    
    @Test
    public void inviteToLobby_FriendNotFound_ReturnsNotFound() throws Exception {
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.inviteToLobby(eq(10L), eq(dummyUser.getId()), any(InviteLobbyRequestDTO.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));

        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(999L);

        mockMvc.perform(
            post("/lobbies/10/invite")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isNotFound());
    }
}
