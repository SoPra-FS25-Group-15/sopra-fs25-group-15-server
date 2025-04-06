package ch.uzh.ifi.hase.soprafs24.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@Service
@Transactional
public class FriendService {

    private final Logger log = LoggerFactory.getLogger(FriendService.class);

    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final AuthService authService; 

    public FriendService(FriendRequestRepository friendRequestRepository,
                         UserRepository userRepository,
                         AuthService authService) {
        this.friendRequestRepository = friendRequestRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    // List incoming friend requests for the authenticated user.
    public List<FriendRequest> getIncomingFriendRequests(String token) {
        User recipient = authService.getUserByToken(token);
        return friendRequestRepository.findByRecipientAndStatus(recipient, FriendRequestStatus.PENDING);
    }

    // Send a friend request from the authenticated user to a recipient.
    public FriendRequest sendFriendRequest(String token, Long recipientId) {
        User sender = authService.getUserByToken(token);
        User recipient = userRepository.findById(recipientId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient not found"));

        // Check that a request doesn't already exist.
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setRecipient(recipient);
        // Status and createdAt are set via @PrePersist.
        friendRequestRepository.save(request);
        log.debug("Friend request sent from {} to {}", sender.getId(), recipient.getId());
        return request;
    }

    // Accept or deny a friend request.
    public FriendRequest respondToFriendRequest(String token, Long requestId, String action) {
        // Only the recipient can respond.
        User recipient = authService.getUserByToken(token);
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));

        if (!request.getRecipient().getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized to respond to this friend request");
        }

        if ("accept".equalsIgnoreCase(action)) {
            request.setStatus(FriendRequestStatus.ACCEPTED);
            log.debug("Friend request {} accepted", requestId);
        } else if ("deny".equalsIgnoreCase(action)) {
            request.setStatus(FriendRequestStatus.DENIED);
            log.debug("Friend request {} denied", requestId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action");
        }

        friendRequestRepository.save(request);
        return request;
    }
    
    // List all friends - now based on accepted friend requests
    public List<User> getFriends(String token) {
        User currentUser = authService.getUserByToken(token);
        List<FriendRequest> acceptedRequests = friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser);
        List<User> friends = new ArrayList<>();
        
        for (FriendRequest request : acceptedRequests) {
            // Only consider accepted requests
            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                if (request.getSender().getId().equals(currentUser.getId())) {
                    friends.add(request.getRecipient());
                } else if (request.getRecipient().getId().equals(currentUser.getId())) {
                    friends.add(request.getSender());
                }
            }
        }
        
        return friends;
    }

    // Remove friendship by finding and deleting the accepted friend request
    public void unfriend(String token, Long friendId) {
        User currentUser = authService.getUserByToken(token);
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend not found"));
        
        List<FriendRequest> requests = friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser);
        FriendRequest friendshipRequest = null;
        
        for (FriendRequest request : requests) {
            // Find the accepted request between these two users
            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                if ((request.getSender().getId().equals(currentUser.getId()) && request.getRecipient().getId().equals(friendId)) ||
                    (request.getRecipient().getId().equals(currentUser.getId()) && request.getSender().getId().equals(friendId))) {
                    friendshipRequest = request;
                    break;
                }
            }
        }
        
        if (friendshipRequest == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found");
        }
        
        friendRequestRepository.delete(friendshipRequest);
        log.debug("Friendship between user {} and friend {} has been removed", currentUser.getId(), friendId);
    }

    // Get all friend requests for the authenticated user (both sent and received)
    public List<FriendRequest> getAllFriendRequests(String token) {
        User currentUser = authService.getUserByToken(token);
        return friendRequestRepository.findBySenderOrRecipient(currentUser, currentUser);
    }

    // Cancel a friend request that was sent by the authenticated user
    public void cancelFriendRequest(String token, Long requestId) {
        User sender = authService.getUserByToken(token);
        FriendRequest request = friendRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
        
        // Only the sender can cancel the request
        if (!request.getSender().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel requests you have sent");
        }
        
        // Only pending requests can be canceled
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Cannot cancel a request that has already been " + request.getStatus().name().toLowerCase());
        }
        
        friendRequestRepository.delete(request);
        log.debug("Friend request {} canceled by sender {}", requestId, sender.getId());
    }

    // Get outgoing friend requests for the authenticated user (only sent requests)
    public List<FriendRequest> getOutgoingFriendRequests(String token) {
        User currentUser = authService.getUserByToken(token);
        return friendRequestRepository.findBySender(currentUser);
    }
}
