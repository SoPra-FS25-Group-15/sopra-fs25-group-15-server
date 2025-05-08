
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.rest.dto.GameRecordDTO;
import java.util.List;

public interface StatsService {
    void saveGameRecord(Long userId, GameRecordDTO record);
    List<GameRecordDTO> getGameRecords(Long userId);

}
