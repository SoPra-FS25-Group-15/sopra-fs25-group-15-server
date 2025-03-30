package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DTOMapper mapper;

    public AuthController(AuthService authService, DTOMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    // 1) Register
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterResponseDTO register(@RequestBody UserRegisterRequestDTO registerDTO) {
        User newUser = mapper.toEntity(registerDTO);
        User created = authService.register(newUser);
        return mapper.toRegisterResponse(created);
    }

    // 2) Login
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public UserLoginResponseDTO login(@RequestBody UserLoginRequestDTO loginDTO) {
        User user = authService.login(loginDTO.getEmail(), loginDTO.getPassword());
        return mapper.toLoginResponse(user);
    }

    // 3) Logout
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public LogoutResponse logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return new LogoutResponse("Logged out successfully.");
    }

    // 4) Get Current Profile
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserMeDTO getMe(@RequestHeader("Authorization") String token) {
        User user = authService.getUserByToken(token);
        return mapper.toUserMeDTO(user);
    }

    // Simple DTO for logout response
    static class LogoutResponse {
        private String message;
        public LogoutResponse(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }
}
