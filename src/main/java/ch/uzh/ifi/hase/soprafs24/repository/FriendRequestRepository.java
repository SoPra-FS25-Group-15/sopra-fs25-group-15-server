package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.constant.FriendRequestStatus;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    // Find incoming friend requests for a recipient that are still pending
    List<FriendRequest> findByRecipientAndStatus(User recipient, FriendRequestStatus status);
    
    // Find outgoing friend requests for a sender
    List<FriendRequest> findBySender(User sender);
    
    // Find all requests where user is either sender or recipient
    List<FriendRequest> findBySenderOrRecipient(User sender, User recipient);
}
