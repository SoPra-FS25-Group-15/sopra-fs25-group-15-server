package ch.uzh.ifi.hase.soprafs24.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.FriendService;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private final FriendService friendService;
    private final DTOMapper mapper;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate; // NEW

    public FriendController(FriendService friendService, DTOMapper mapper, 
                            AuthService authService, SimpMessagingTemplate messagingTemplate) {
        this.friendService = friendService;
        this.mapper = mapper;
        this.authService = authService;
        this.messagingTemplate = messagingTemplate;
    }

    // GET /friends/requests
    @GetMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<FriendRequestDTO> getFriendRequests(@RequestHeader("Authorization") String token) {
        List<FriendRequest> requests = friendService.getIncomingFriendRequests(token);
        return requests.stream()
                .map(mapper::toFriendRequestDTO)
                .collect(Collectors.toList());
    }

    // POST /friends/request - send a friend request and notify recipient via WS
    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public FriendRequestDTO sendFriendRequest(@RequestHeader("Authorization") String token,
                                              @RequestBody FriendRequestDTO requestDTO) {
        FriendRequest request = friendService.sendFriendRequest(token, requestDTO.getRecipient());
        FriendRequestDTO responseDTO = mapper.toFriendRequestDTO(request);

        // Use the session token (or unique identifier) from the recipient to send the WS message.
        User recipient = request.getRecipient();
        if (recipient != null && recipient.getToken() != null) {
            messagingTemplate.convertAndSendToUser(
                    recipient.getToken(),      // user identifier
                    "/queue/friendRequest",    // destination
                    responseDTO                // payload
            );
        }

        return responseDTO;
    }

    // PUT /friends/requests/{requestId} - respond to a friend request and notify sender
    @PutMapping("/requests/{requestId}")
    @ResponseStatus(HttpStatus.OK)
    public FriendRequestDTO respondToFriendRequest(@RequestHeader("Authorization") String token,
                                                   @PathVariable Long requestId,
                                                   @RequestBody FriendRequestDTO requestDTO) {
        FriendRequest request = friendService.respondToFriendRequest(token, requestId, requestDTO.getAction());
        FriendRequestDTO responseDTO = mapper.toFriendRequestDTO(request);

        // Notify the sender about the response (accept/deny)
        User sender = request.getSender();
        if (sender != null && sender.getToken() != null) {
            messagingTemplate.convertAndSendToUser(
                    sender.getToken(),
                    "/queue/friendRequestResponse",
                    responseDTO
            );
        }

        return responseDTO;
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
        List<FriendRequest> requests = friendService.getOutgoingFriendRequests(token);
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
