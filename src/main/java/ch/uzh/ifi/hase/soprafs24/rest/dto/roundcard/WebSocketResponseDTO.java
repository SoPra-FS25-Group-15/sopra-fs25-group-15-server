package ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WebSocket responses
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketResponseDTO {
    private String type;
    private String message;
    private Game game;
}
