package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.actioncard.ActionCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.ActionCardService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActionCardControllerTest {

    @Mock
    private ActionCardService actionCardService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ActionCardController actionCardController;

    private static final String TEST_TOKEN = "test-token";
    private static final Long TEST_GAME_ID = 1L;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void drawRandom_validToken_returnsCard() {
        // Setup mocks
        User mockUser = new User();
        mockUser.setId(1L);
        when(userService.getUserByToken(TEST_TOKEN)).thenReturn(mockUser);
        
        ActionCardDTO mockCard = new ActionCardDTO();
        mockCard.setId("7choices");
        mockCard.setType("powerup");
        when(actionCardService.drawRandomCard()).thenReturn(mockCard);
        
        // Call method
        ActionCardDTO result = actionCardController.drawRandom(TEST_GAME_ID, TEST_TOKEN);
        
        // Verify
        verify(userService).getUserByToken(TEST_TOKEN);
        verify(actionCardService).drawRandomCard();
        assertEquals(mockCard, result);
    }
    
    @Test
    void drawRandom_invalidToken_throwsException() {
        // Setup mocks
        when(userService.getUserByToken(TEST_TOKEN))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        
        // Call method and check exception
        assertThrows(ResponseStatusException.class, () -> {
            actionCardController.drawRandom(TEST_GAME_ID, TEST_TOKEN);
        });
        
        // Verify
        verify(userService).getUserByToken(TEST_TOKEN);
        verify(actionCardService, never()).drawRandomCard();
    }
}
