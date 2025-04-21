package ch.uzh.ifi.hase.soprafs24.rest.dto.roundcard;

import ch.uzh.ifi.hase.soprafs24.constant.RoundCardType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundCardDTO {

    private Long id;
    private String name;
    private String description;
    private RoundCardType type;
    private String guessType;
    private String streetViewType;
    private Integer roundTimeInSeconds;
    private String mapType;
}