package ch.uzh.ifi.hase.soprafs24.repository;
import java.util.List; 
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, Long> {
    // Lookup via the associated UserProfile's username:
    User findByProfile_Username(String username);
    // Lookup via email and token:
    User findByEmail(String email);
    User findByToken(String token);
    List<User> findTop10ByOrderByProfileMmrDesc();

}
