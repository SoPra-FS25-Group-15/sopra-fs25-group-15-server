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
import ch.uzh.ifi.hase.soprafs24.entity.Friendship;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import ch.uzh.ifi.hase.soprafs24.repository.FriendshipRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

@Service
@Transactional
public class FriendService {

    private final Logger log = LoggerFactory.getLogger(FriendService.class);

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public FriendService(FriendRequestRepository friendRequestRepository,
                         FriendshipRepository friendshipRepository,
                         UserRepository userRepository,
                         AuthService authService) {
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
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
        // (You could add more checks here if needed.)
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
            // Create a Friendship record.
            Friendship friendship = new Friendship();
            friendship.setUser1(request.getSender());
            friendship.setUser2(request.getRecipient());
            friendshipRepository.save(friendship);
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
    
    // list all friends
    public List<User> getFriends(String token) {
        User currentUser = authService.getUserByToken(token);
        // For demonstration, we retrieve all friendships and filter them.
        // In production, consider writing a custom query to fetch only relevant friendships.
        List<Friendship> allFriendships = friendshipRepository.findAll();
        List<User> friends = new ArrayList<>();
        
        for (Friendship fs : allFriendships) {
            if (fs.getUser1().getId().equals(currentUser.getId())) {
                friends.add(fs.getUser2());
            } else if (fs.getUser2().getId().equals(currentUser.getId())) {
                friends.add(fs.getUser1());
            }
        }
        
        return friends;
    }


    //remove friend
    public void unfriend(String token, Long friendId) {

        User currentUser = authService.getUserByToken(token);
        


        List<Friendship> allFriendships = friendshipRepository.findAll();
        Friendship friendshipToRemove = null;
        
        for (Friendship fs : allFriendships) {
            if (fs.getUser1().getId().equals(currentUser.getId()) && fs.getUser2().getId().equals(friendId)) {
                friendshipToRemove = fs;
                break;
            } else if (fs.getUser2().getId().equals(currentUser.getId()) && fs.getUser1().getId().equals(friendId)) {
                friendshipToRemove = fs;
                break;
            }
        }
        
        if (friendshipToRemove == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found");
        }
        

        friendshipRepository.delete(friendshipToRemove);
        friendshipRepository.flush();
        log.debug("Friendship between user {} and friend {} has been removed", currentUser.getId(), friendId);
    }
}
