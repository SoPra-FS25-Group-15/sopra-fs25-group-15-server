package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void inviteToLobby_ValidRequest_ReturnsInviteResponse() throws Exception {
        // given
        InviteLobbyRequestDTO inviteDTO = new InviteLobbyRequestDTO();
        inviteDTO.setFriendId(2L);
        
        // Create an instance of LobbyInviteResponseDTO with the correct fields
        LobbyInviteResponseDTO inviteResponseDTO = new LobbyInviteResponseDTO();
        inviteResponseDTO.setInvitedFriend("friendUsername"); // This is what the actual DTO has
        inviteResponseDTO.setLobbyCode(dummyLobby.getLobbyCode());
        
        when(authService.getUserByToken(token)).thenReturn(dummyUser);
        when(lobbyService.inviteToLobby(eq(10L), eq(dummyUser.getId()), Mockito.any(InviteLobbyRequestDTO.class)))
            .thenReturn(inviteResponseDTO);
        
        // when/then
        mockMvc.perform(
            post("/lobbies/10/invite")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inviteDTO)))
            .andExpect(status().isOk())
            // Update assertions to match actual response structure
            .andExpect(jsonPath("$.invitedFriend").value("friendUsername"))
            .andExpect(jsonPath("$.lobbyCode").value(dummyLobby.getLobbyCode()));
    }
}
