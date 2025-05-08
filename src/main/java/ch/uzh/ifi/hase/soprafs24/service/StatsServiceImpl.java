package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.GameRecord;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRecordRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameRecordDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StatsService, handling persistence and retrieval of game records.
 */
@Service
@Transactional
public class StatsServiceImpl implements StatsService {

    private final GameRecordRepository recordRepo;
    private final UserRepository        userRepo;

    public StatsServiceImpl(GameRecordRepository recordRepo,
                            UserRepository userRepo) {
        this.recordRepo = recordRepo;
        this.userRepo   = userRepo;
    }

    @Override
    public void saveGameRecord(Long userId, GameRecordDTO dto) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        GameRecord record = new GameRecord();
        record.setUser(user);
        record.setWinner(dto.getWinner());
        record.setPlayers(dto.getPlayers());
        record.setRoundsPlayed(dto.getRoundsPlayed());
        record.setRoundCardStartAmount(dto.getRoundCardStartAmount());
        record.setStartedAt(dto.getStartedAt());
        record.setCompletedAt(dto.getCompletedAt());

        recordRepo.save(record);
    }

    @Override
    public List<GameRecordDTO> getGameRecords(Long userId) {
        return recordRepo.findByUserId(userId).stream()
            .map(rec -> {
                GameRecordDTO dto = new GameRecordDTO();
                dto.setWinner(rec.getWinner());
                dto.setPlayers(rec.getPlayers());
                dto.setRoundsPlayed(rec.getRoundsPlayed());
                dto.setRoundCardStartAmount(rec.getRoundCardStartAmount());
                dto.setStartedAt(rec.getStartedAt());
                dto.setCompletedAt(rec.getCompletedAt());
                return dto;
            })
            .collect(Collectors.toList());
    }
}
