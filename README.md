# Summary ActionGuessr

### Introduction

ActionGuessr is a turn-based game designed as an innovative and strategic enhancement of the popular game GeoGuessr. The motivation behind ActionGuessr is to introduce strategic depth and dynamic player interaction, significantly enriching the original gameplay experience. To achieve this, we implemented a system of round cards that determine the unique conditions under which each round is played. Players are also given action cards, which they strategically deploy either as beneficial power-ups for themselves or as tactical punishments targeting their opponents. The ultimate objective is to discard all round cards, with the first player to achieve this being declared the winner. Additionally, winning a round provides the player the advantage of selecting the next round's card, but a card can only be permanently discarded by winning two consecutive rounds. This structure ensures a captivating, engaging, and strategically rewarding gameplay experience beyond the original GeoGuessr concept.

### Technologies 

- Java: Core programming language utilized for robust backend development, offering platform-independent functionality.
- Java Persistence API (JPA): Provides object-relational mapping for managing relational data in a Java application.
- Spring: Framework used for dependency injection, web service creation, and RESTful APIs, ensuring modularity and streamlined backend development.
- Gradle: Powerful build automation tool that simplifies the process of compiling, building, and testing the application.
- Docker: Containerization technology that ensures consistency and ease in managing dependencies and environments across development, testing, and production phases.
- Sockjs: JavaScript library ensuring reliable, cross-browser communication, emulating WebSockets with fallback options like HTTP polling.
- Google Cloud: Scalable cloud services platform offering infrastructure, computing resources, and data storage solutions.
- PostgreSQL Database: Open-source relational database system known for reliability, extensibility, and robust SQL compliance.

### High-level components

#### Gamewebsocket Controller
[Gamewebsocket Controller Code](https://github.com/SoPra-FS25-Group-15/sopra-fs25-group-15-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/websocket/controller/GameWebSocketController.java)
Manages real-time communication between server and clients, orchestrating core game logic and maintaining seamless interactive gameplay.

#### Game Service
[Game Service Code](https://github.com/SoPra-FS25-Group-15/sopra-fs25-group-15-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameService.java)
Handles the business logic related to game mechanics, including managing player states, game progression, and calculating experience points (XP).

#### Lobbywebsocket Controller
[Lobbywebsocket Controller Code](https://github.com/SoPra-FS25-Group-15/sopra-fs25-group-15-server/blob/main/src/main/java/ch/uzh/ifi/hase/soprafs24/websocket/controller/LobbyWebSocketController.java)
Facilitates the creation and management of game lobbies, enabling users to set up games, join sessions, and maintain player readiness and lobby synchronization.


### Launch & Deployement

#### Local Development
1. Clone the repository: ```git clone <repository-url>```
2. Navigate to the project directory: ```cd <project-directory>```
3. Build the application locally: ```./gradlew build```
4. Run the application locally:  ```./gradlew bootRun --args='--spring.profiles.active=dev --spring.cloud.gcp.sql.enabled=false'```

#### Run the tests
To run the tests, use: ```./gradlew test```

#### Deployement
Deployment is automated upon pushing to the main branch.


### Roadmap

- Enhanced Reliability: Improve server stability and performance for handling increased player load.
- Player Statistics: Integrate backend stats tracking games played and won.

### Authors

Julien Zbinden, Flavia Röösli, Daria Stetsenko, Theodor Mattli, Zhenmei Hao and Tongxi Hu

### Aknowledgment

Thank you to our TA, Lucas Bär, who has been a great help in order to make this project work. Also a big thank you to the whole SoPra Team, who supported us in the process of developing this project and providing a great template.

### License

This project is licensed under the MIT License - see the [LICENSE.md](https://github.com/SoPra-FS25-Group-15/sopra-fs25-group-15-server/blob/main/license.md) file for details


