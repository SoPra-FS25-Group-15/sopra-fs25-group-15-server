package ch.uzh.ifi.hase.soprafs24.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.websocket.dto.PlayerXpUpdateDTO;

public class UserXpServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @InjectMocks
    private UserXpService userXpService;
    
    private User testUser;
    private static final String TEST_TOKEN = "test-token";
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken(TEST_TOKEN);
        
        // Create user profile
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        profile.setXp(100);
        testUser.setProfile(profile);
        
        // Mock repository save
        when(userRepository.save(any(User.class))).thenReturn(testUser);
    }
    
    @Test
    public void testAwardXp_IncrementsUserXp() {
        // Initial XP
        int initialXp = testUser.getProfile().getXp();
        int xpToAward = 20;
        
        // Call the method
        userXpService.awardXp(testUser, xpToAward, "Test reason");
        
        // Verify XP was updated
        assertEquals(initialXp + xpToAward, testUser.getProfile().getXp());
        
        // Verify user was saved to repository
        verify(userRepository, times(1)).save(eq(testUser));
        
        // Verify notification was sent
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq(TEST_TOKEN),
            eq("/queue/xp-updates"),
            any(PlayerXpUpdateDTO.class)
        );
    }
    
    @Test
    public void testAwardXpForGuess_UsesCorrectXpAmount() {
        userXpService.awardXpForGuess(testUser);
        
        // Verify XP was updated with correct amount
        assertEquals(100 + UserXpService.XP_FOR_GUESS, testUser.getProfile().getXp());
    }
    
    @Test
    public void testAwardXpForRoundWin_UsesCorrectXpAmount() {
        userXpService.awardXpForRoundWin(testUser);
        
        // Verify XP was updated with correct amount
        assertEquals(100 + UserXpService.XP_FOR_ROUND_WIN, testUser.getProfile().getXp());
    }
    
    @Test
    public void testAwardXpForGameWin_UsesCorrectXpAmount() {
        userXpService.awardXpForGameWin(testUser);
        
        // Verify XP was updated with correct amount
        assertEquals(100 + UserXpService.XP_FOR_GAME_WIN, testUser.getProfile().getXp());
    }
    
    @Test
    public void testSendCurrentXpNotification_SendsCorrectData() {
        userXpService.sendCurrentXpNotification(testUser);
        
        // Verify notification was sent with correct data
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq(TEST_TOKEN),
            eq("/queue/xp-updates"),
            any(PlayerXpUpdateDTO.class)
        );
    }
}
