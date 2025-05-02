package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.ANY;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;

@DataJpaTest(properties = {
  // turn off the Cloud SQL post-processor
  "spring.cloud.gcp.sql.enabled=false"
})
@AutoConfigureTestDatabase(replace = ANY)
public class UserRepositoryIntegrationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Test
  public void findByEmail_success() {
    // given
    User user = new User();
    user.setEmail("firstname.lastname@example.com");
    user.setPassword("password");
    user.setToken("1");
    user.setStatus(UserStatus.OFFLINE);
    
    UserProfile profile = new UserProfile();
    profile.setUsername("Firstname Lastname");
    profile.setMmr(1500);
    profile.setAchievements(Arrays.asList("Achievement1", "Achievement2"));
    // You can set other profile fields if necessary.
    user.setProfile(profile);

    entityManager.persist(user);
    entityManager.flush();

    // when
    User found = userRepository.findByEmail(user.getEmail());

    // then
    assertNotNull(found.getId());
    assertEquals(user.getEmail(), found.getEmail());
    assertEquals(user.getProfile().getUsername(), found.getProfile().getUsername());
    assertEquals(user.getToken(), found.getToken());
    assertEquals(user.getStatus(), found.getStatus());
  }
}