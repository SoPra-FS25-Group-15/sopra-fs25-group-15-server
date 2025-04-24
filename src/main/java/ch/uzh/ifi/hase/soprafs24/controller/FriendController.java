package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;
    private final DTOMapper mapper;
    private final AuthService authService;
    private final UserService userService;

    public FriendController(FriendService friendService, DTOMapper mapper, AuthService authService,
            UserService userService) {
        this.friendService = friendService;
        this.mapper = mapper;
        this.authService = authService;
        this.userService = userService;
    }

    // GET /api/friends/requests - get incoming friend requests for the
    // authenticated user
    @GetMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<FriendRequestDTO> getFriendRequests(@RequestHeader("Authorization") String token) {
        List<FriendRequest> requests = friendService.getIncomingFriendRequests(token);
        return requests.stream()
                .map(mapper::toFriendRequestDTO)
                .collect(Collectors.toList());
    }

    // POST /api/friends/request - send a friend request
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> sendFriendRequest(@RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {

        // Find recipient by username or email
        String searchQuery = body.get("query");
        User recipient = userService.getUserBySearch(searchQuery);

        // Check if the recipient is the same as the sender
        User sender = authService.getUserByToken(token);
        if (recipient.getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }

        friendService.sendFriendRequest(token, recipient.getId());
        return Map.of("message", "Friend request sent to " + recipient.getProfile().getUsername());
    }

    // PUT /api/friends/requests/{requestId} - respond to a friend request (accept
    // or deny)
    @PutMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public FriendRequestDTO respondToFriendRequest(@RequestHeader("Authorization") String token,
            @PathVariable Long requestId,
            @RequestBody FriendRequestDTO requestDTO) {
        FriendRequest request = friendService.respondToFriendRequest(token, requestId, requestDTO.getAction());
        return mapper.toFriendRequestDTO(request);
    }

    // GET /api/friends - list friends for the authenticated user
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<FriendDTO> listFriends(@RequestHeader("Authorization") String token) {
        List<User> friends = friendService.getFriends(token);
        return friends.stream()
                .map(mapper::toFriendDTO)
                .collect(Collectors.toList());
    }

    // DELETE /api/friends/{friendId} - Unfriend a user.
    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfriend(@RequestHeader("Authorization") String token,
            @PathVariable Long friendId) {
        friendService.unfriend(token, friendId);
    }

    // GET /api/friends/all-requests - get all friend requests (sent and received)
    @GetMapping("/all-requests")
    @ResponseStatus(HttpStatus.OK)
    public List<FriendRequestDTO> getAllFriendRequests(@RequestHeader("Authorization") String token) {
        List<FriendRequest> requests = new ArrayList<>();
        requests.addAll(friendService.getOutgoingFriendRequests(token));
        requests.addAll(friendService.getIncomingFriendRequests(token));
        User currentUser = authService.getUserByToken(token);
        return requests.stream()
                .map(request -> mapper.toFriendRequestDTO(request, currentUser))
                .collect(Collectors.toList());
    }

    // DELETE /api/friends/requests/{requestId} - cancel a sent friend request
    @DeleteMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelFriendRequest(@RequestHeader("Authorization") String token,
            @PathVariable Long requestId) {
        friendService.cancelFriendRequest(token, requestId);
    }
}
