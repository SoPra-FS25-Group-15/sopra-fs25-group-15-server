package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.rest.dto.game.RoundCardDTO;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.RoundCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug controller for troubleshooting game issues
 * ONLY ENABLED IN DEV AND TEST ENVIRONMENTS
 */
@RestController
@RequestMapping("/debug")
public class DebugController {

    private static final Logger log = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private GameService gameService;
    
    @Autowired
    private RoundCardService roundCardService;
    
    @Autowired
    private AuthService authService;

    @GetMapping("/game/{gameId}/state")
    public ResponseEntity<Object> getGameState(@PathVariable Long gameId) {
        try {
            GameService.GameState state = gameService.getGameState(gameId);
            if (state == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Game not found"));
            }
            
            // Convert to a serializable map
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put("currentRound", state.getCurrentRound());
            stateMap.put("currentScreen", state.getCurrentScreen());
            stateMap.put("roundCardSubmitter", state.getRoundCardSubmitter());
            stateMap.put("activeRoundCard", state.getActiveRoundCard());
            stateMap.put("currentTurnPlayerToken", state.getCurrentTurnPlayerToken());
            
            // Get guess screen attributes
            Map<String, Object> guessAttrs = new HashMap<>();
            var attrs = state.getGuessScreenAttributes();
            guessAttrs.put("time", attrs.getTime());
            guessAttrs.put("latitude", attrs.getLatitude());
            guessAttrs.put("longitude", attrs.getLongitude());
            stateMap.put("guessScreenAttributes", guessAttrs);
            
            log.info("Debug: Retrieved game state for game {}", gameId);
            return ResponseEntity.ok(stateMap);
        } catch (Exception e) {
            log.error("Debug: Error getting game state: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/game/{gameId}/player/{playerToken}/cards")
    public ResponseEntity<Object> getPlayerCards(@PathVariable Long gameId,
                                               @PathVariable String playerToken,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            authService.getUserByToken(token); // Verify token is valid
            
            List<RoundCardDTO> roundCards = roundCardService.getPlayerRoundCardsByToken(gameId, playerToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("roundCards", roundCards);
            
            log.info("Debug: Retrieved {} round cards for player {} in game {}", roundCards.size(), playerToken, gameId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Debug: Error getting player cards: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/game/{gameId}/select-round-card")
    public ResponseEntity<Object> selectRoundCard(@PathVariable Long gameId,
                                                @RequestBody Map<String, String> payload,
                                                @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            authService.getUserByToken(token); // Verify token is valid
            
            String roundCardId = payload.get("roundCardId");
            String playerToken = payload.get("playerToken");
            
            if (roundCardId == null || playerToken == null) {
                return ResponseEntity.badRequest()
                                    .body(Map.of("error", "roundCardId and playerToken are required"));
            }
            
            GameService.GameState gameState = gameService.getGameState(gameId);
            if (gameState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Game not found"));
            }
            
            // Find the round card
            List<RoundCardDTO> playerCards = roundCardService.getPlayerRoundCardsByToken(gameId, playerToken);
            RoundCardDTO selectedCard = playerCards.stream()
                .filter(card -> card.getId().equals(roundCardId))
                .findFirst()
                .orElse(null);
                
            if (selectedCard == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("error", "Round card not found"));
            }
            
            // Start round with this card
            gameService.startRound(gameId, selectedCard);
            
            // Send game state to all players
            gameService.sendGameStateToAll(gameId);
            
            log.info("Debug: Selected round card {} for player {} in game {}", roundCardId, playerToken, gameId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Round card selected successfully"
            ));
        } catch (Exception e) {
            log.error("Debug: Error selecting round card: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/game/{gameId}/force-round-card")
    public ResponseEntity<Object> forceRoundCardSelection(@PathVariable Long gameId,
                                               @RequestBody Map<String, String> payload) {
        try {
            String playerToken = payload.get("playerToken");
            String roundCardId = payload.get("roundCardId");
            
            if (playerToken == null || roundCardId == null) {
                return ResponseEntity.badRequest()
                                   .body(Map.of("error", "playerToken and roundCardId are required"));
            }
            
            GameService.GameState gameState = gameService.getGameState(gameId);
            if (gameState == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                   .body(Map.of("error", "Game not found"));
            }
            
            // Find the round card
            List<RoundCardDTO> playerCards = roundCardService.getPlayerRoundCardsByToken(gameId, playerToken);
            RoundCardDTO selectedCard = playerCards.stream()
                .filter(card -> card.getId().equals(roundCardId))
                .findFirst()
                .orElse(null);
                
            if (selectedCard == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                   .body(Map.of("error", "Round card not found"));
            }
            
            // Force update the active round card
            gameState.setActiveRoundCard(roundCardId);
            
            // Force coordinates generation
            var coordinates = gameService.startRound(gameId, selectedCard);
            
            // Send game state to all players
            gameService.sendGameStateToAll(gameId);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Round card forced",
                "coordinates", Map.of(
                    "latitude", coordinates.getLatitude(),
                    "longitude", coordinates.getLongitude()
                )
            ));
        } catch (Exception e) {
            log.error("Debug: Error forcing round card: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", e.getMessage()));
        }
    }
}
