package ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for round card submission via WebSocket
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoundCardSubmitDTO {
    private String token;
    private Long roundCardId;
    private Long gameId;
}
