package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserMeDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPublicDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserRegisterResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserStatsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserUpdateResponseDTO;

public class DTOMapperTest {

    private DTOMapper mapper;
    private User dummyUser;

    @BeforeEach
    public void setup() {
        mapper = new DTOMapper();
        dummyUser = new User();
        
        // Use reflection to set the ID field directly
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dummyUser, 100L);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set user ID via reflection", e);
        }
        
        // Continue with the rest of setup
        dummyUser.setEmail("user@example.com");
        dummyUser.setPassword("secret");
        dummyUser.setToken("dummy-token");
        dummyUser.setStatus(UserStatus.OFFLINE);
        dummyUser.setCreatedAt(Instant.now());
    
        UserProfile profile = new UserProfile();
        profile.setUsername("dummyUser");
        profile.setMmr(1500);
        profile.setAchievements(Arrays.asList("First Win", "Sharp Shooter"));
        profile.setFriends(new ArrayList<>());
        profile.setStatsPublic(true);
        dummyUser.setProfile(profile);
    }

    @Test
    public void testToEntity_fromUserRegisterRequestDTO() {
        UserRegisterRequestDTO registerDTO = new UserRegisterRequestDTO();
        registerDTO.setUsername("newUser");
        registerDTO.setEmail("new@example.com");
        registerDTO.setPassword("newSecret");

        User newUser = mapper.toEntity(registerDTO);

        assertEquals(registerDTO.getUsername(), newUser.getProfile().getUsername());
        assertEquals(registerDTO.getEmail(), newUser.getEmail());
        assertEquals(registerDTO.getPassword(), newUser.getPassword());
        // defaults
        assertEquals(0, newUser.getProfile().getMmr());
        assertNotNull(newUser.getProfile().getAchievements());
    }

    @Test
    public void testToRegisterResponse() {
        UserRegisterResponseDTO responseDTO = mapper.toRegisterResponse(dummyUser);

        assertEquals(dummyUser.getId(), responseDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), responseDTO.getUsername());
        assertEquals(dummyUser.getEmail(), responseDTO.getEmail());
        assertEquals(dummyUser.getToken(), responseDTO.getToken());
        assertEquals(dummyUser.getCreatedAt(), responseDTO.getCreatedAt());
    }

    @Test
    public void testToLoginResponse() {
        UserLoginResponseDTO loginDTO = mapper.toLoginResponse(dummyUser);

        assertEquals(dummyUser.getId(), loginDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), loginDTO.getUsername());
        assertEquals(dummyUser.getToken(), loginDTO.getToken());
        // points interpreted as mmr
        assertEquals(dummyUser.getProfile().getMmr(), loginDTO.getPoints());
    }

    @Test
    public void testToUserMeDTO() {
        UserMeDTO meDTO = mapper.toUserMeDTO(dummyUser);

        assertEquals(dummyUser.getId(), meDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), meDTO.getUsername());
        assertEquals(dummyUser.getEmail(), meDTO.getEmail());
        assertEquals(dummyUser.getToken(), meDTO.getToken());
    }

    @Test
    public void testToUserPublicDTO() {
        UserPublicDTO publicDTO = mapper.toUserPublicDTO(dummyUser);

        assertEquals(dummyUser.getId(), publicDTO.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), publicDTO.getUsername());
        assertEquals(dummyUser.getProfile().getMmr(), publicDTO.getMmr());
        assertEquals(dummyUser.getProfile().getAchievements(), publicDTO.getAchievements());
    }

    @Test
    public void testUpdateEntityFromDTO() {
        UserUpdateRequestDTO updateDTO = new UserUpdateRequestDTO();
        updateDTO.setUsername("updatedUser");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setStatsPublic(false);

        // Before update: use dummyUser's original data.
        mapper.updateEntityFromDTO(dummyUser, updateDTO);

        assertEquals("updatedUser", dummyUser.getProfile().getUsername());
        assertEquals("updated@example.com", dummyUser.getEmail());
        assertEquals(false, dummyUser.getProfile().isStatsPublic());
    }

    @Test
    public void testToUpdateResponse() {
        UserUpdateResponseDTO updateResp = mapper.toUpdateResponse(dummyUser);

        assertEquals(dummyUser.getId(), updateResp.getUserid());
        assertEquals(dummyUser.getProfile().getUsername(), updateResp.getUsername());
        assertEquals(dummyUser.getEmail(), updateResp.getEmail());
    }

    @Test
    public void testToUserStatsDTO() {
        // Set additional stats in user profile
        dummyUser.getProfile().setGamesPlayed(20);
        dummyUser.getProfile().setWins(12);
        dummyUser.getProfile().setMmr(1550);

        UserStatsDTO statsDTO = mapper.toUserStatsDTO(dummyUser);

        assertEquals(20, statsDTO.getGamesPlayed());
        assertEquals(12, statsDTO.getWins());
        assertEquals(1550, statsDTO.getMmr());
        // Note: points may be left at default (0) if not set explicitly in mapper 
    }

    @Test
    public void testToFriendRequestDTO() {
        // create sender and recipient users using reflection to set the ID
        
        // Sender
        User sender = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sender, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile senderProfile = new UserProfile();
        senderProfile.setUsername("senderUser");
        sender.setProfile(senderProfile);

        // Recipient
        User recipient = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(recipient, 2L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile recipientProfile = new UserProfile();
        recipientProfile.setUsername("recipientUser");
        recipient.setProfile(recipientProfile);

        FriendRequest friendRequest = new FriendRequest();
        // manually set id and status
        friendRequest.setStatus(FriendRequestStatus.PENDING);
        // set sender and recipient
        friendRequest.setSender(sender);
        friendRequest.setRecipient(recipient);
        // Use reflection to set id of friendRequest to 10L
        try {
            Field idField = FriendRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friendRequest, 10L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        FriendRequestDTO frDTO = mapper.toFriendRequestDTO(friendRequest);
        assertEquals(10L, frDTO.getRequestId());
        // FriendRequestDTO.recipient is set to the recipient id.
        assertEquals(recipient.getId(), frDTO.getRecipient());
        // Status converted to lower-case string (example: "pending")
        assertEquals("pending", frDTO.getAction());
    }

    @Test
    public void testToFriendDTO() {
        User friend = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(friend, 55L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        UserProfile friendProfile = new UserProfile();
        friendProfile.setUsername("bestFriend");
        friend.setProfile(friendProfile);

        FriendDTO friendDTO = mapper.toFriendDTO(friend);
        assertEquals(friend.getId(), friendDTO.getFriendId());
        assertEquals(friend.getProfile().getUsername(), friendDTO.getUsername());
    }
}