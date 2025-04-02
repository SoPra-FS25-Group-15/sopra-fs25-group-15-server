package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final DTOMapper mapper;

    public FriendController(FriendService friendService, DTOMapper mapper) {
        this.friendService = friendService;
        this.mapper = mapper;
    }

    // GET /api/friends/requests - get incoming friend requests for the authenticated user
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
    public FriendRequestDTO sendFriendRequest(@RequestHeader("Authorization") String token,
                                              @RequestBody FriendRequestDTO requestDTO) {
        FriendRequest request = friendService.sendFriendRequest(token, requestDTO.getRecipient());
        return mapper.toFriendRequestDTO(request);
    }

    // PUT /api/friends/requests/{requestId} - respond to a friend request (accept or deny)
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
}
