package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DTOMapper {

    // 1) Registration
    public User toEntity(UserRegisterRequestDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        
        // Default status assigned in service or here
        user.setStatus(ch.uzh.ifi.hase.soprafs24.constant.UserStatus.OFFLINE);

        // Build profile
        UserProfile profile = new UserProfile();
        profile.setUsername(dto.getUsername());
        profile.setMmr(0); // default
        profile.setAchievements(new ArrayList<>());
        user.setProfile(profile);

        return user;
    }

    public UserRegisterResponseDTO toRegisterResponse(User user) {
        UserRegisterResponseDTO dto = new UserRegisterResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        dto.setToken(user.getToken());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    // 2) Login
    public UserLoginResponseDTO toLoginResponse(User user) {
        UserLoginResponseDTO dto = new UserLoginResponseDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setToken(user.getToken());
        dto.setPoints(user.getProfile().getMmr()); // interpret "points" as mmr
        return dto;
    }

    // 3) Get Current Profile (/api/auth/me)
    public UserMeDTO toUserMeDTO(User user) {
        UserMeDTO dto = new UserMeDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setEmail(user.getEmail());
        dto.setToken(user.getToken());
        return dto;
    }

    // 4) Get Public Profile
    public UserPublicDTO toUserPublicDTO(User user) {
        UserPublicDTO dto = new UserPublicDTO();
        dto.setUserid(user.getId());
        dto.setUsername(user.getProfile().getUsername());
        dto.setMmr(user.getProfile().getMmr());
        dto.setAchievements(user.getProfile().getAchievements());
        return dto;}}