package ch.uzh.ifi.hase.soprafs24.controller;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;

public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private DTOMapper dtoMapper;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private UserRegisterRequestDTO registerRequestDTO;
    private UserLoginRequestDTO loginRequestDTO;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test user
        testUser = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setToken("test-token");
        testUser.setStatus(UserStatus.ONLINE);
        testUser.setCreatedAt(Instant.now());

        // Set up user profile
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        testUser.setProfile(profile);

        // Create test DTOs
        registerRequestDTO = new UserRegisterRequestDTO();
        registerRequestDTO.setEmail("test@example.com");
        registerRequestDTO.setUsername("testUser");
        registerRequestDTO.setPassword("password123");

        loginRequestDTO = new UserLoginRequestDTO();
        loginRequestDTO.setEmail("test@example.com");
        loginRequestDTO.setPassword("password123");
    }

    @Test
    public void testRegister_Success() {
        // Arrange
        UserRegisterResponseDTO expectedResponse = new UserRegisterResponseDTO();
        expectedResponse.setUserid(1L);
        expectedResponse.setToken("test-token");

        when(dtoMapper.toEntity(any(UserRegisterRequestDTO.class))).thenReturn(testUser);
        when(authService.register(any(User.class))).thenReturn(testUser);
        when(dtoMapper.toRegisterResponse(any(User.class))).thenReturn(expectedResponse);

        // Act
        UserRegisterResponseDTO actualResponse = authController.register(registerRequestDTO);

        // Assert
        assertEquals(expectedResponse.getUserid(), actualResponse.getUserid());
        assertEquals(expectedResponse.getToken(), actualResponse.getToken());
        Mockito.verify(authService).register(any(User.class));
    }

    @Test
    public void testLogin_Success() {
        // Arrange
        UserLoginResponseDTO expectedResponse = new UserLoginResponseDTO();
        expectedResponse.setUserid(1L);
        expectedResponse.setToken("test-token");
        expectedResponse.setUsername("testUser");

        when(authService.login(eq("test@example.com"), eq("password123"))).thenReturn(testUser);
        when(dtoMapper.toLoginResponse(testUser)).thenReturn(expectedResponse);

        // Act
        UserLoginResponseDTO actualResponse = authController.login(loginRequestDTO);

        // Assert
        assertEquals(expectedResponse.getUserid(), actualResponse.getUserid());
        assertEquals(expectedResponse.getToken(), actualResponse.getToken());
        assertEquals(expectedResponse.getUsername(), actualResponse.getUsername());
        Mockito.verify(authService).login("test@example.com", "password123");
    }

    @Test
    public void testLogout_Success() {
        // Act
        AuthController.LogoutResponse response = authController.logout("test-token");

        // Assert
        assertEquals("Logged out successfully.", response.getMessage());
        Mockito.verify(authService).logout("test-token");
    }

    @Test
    public void testGetMe_Success() {
        // Arrange
        UserMeDTO expectedResponse = new UserMeDTO();
        expectedResponse.setUserid(1L);
        expectedResponse.setUsername("testUser");
        expectedResponse.setEmail("test@example.com");

        when(authService.getUserByToken("test-token")).thenReturn(testUser);
        when(dtoMapper.toUserMeDTO(testUser)).thenReturn(expectedResponse);

        // Act
        UserMeDTO actualResponse = authController.getMe("test-token");

        // Assert
        assertEquals(expectedResponse.getUserid(), actualResponse.getUserid());
        assertEquals(expectedResponse.getUsername(), actualResponse.getUsername());
        assertEquals(expectedResponse.getEmail(), actualResponse.getEmail());
        Mockito.verify(authService).getUserByToken("test-token");
    }
}