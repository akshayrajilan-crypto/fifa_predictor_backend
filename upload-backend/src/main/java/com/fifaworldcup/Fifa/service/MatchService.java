package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.MatchResponse;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

    public List<MatchResponse> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateTimeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getMatchesByStage(Match.Stage stage) {
        return matchRepository.findByStage(stage).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getMatchesByGroup(String group) {
        return matchRepository.findByGroupOrderByMatchDateTimeAsc(group).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getTodaysMatches() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return matchRepository.findByMatchDateTimeBetween(startOfDay, endOfDay).stream()
                .map(this::toResponse)
                .toList();
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return toResponse(match);
    }

    private MatchResponse toResponse(Match match) {
        return MatchResponse.builder()
                .id(match.getId())
                .team1Id(match.getTeam1().getId())
                .team2Id(match.getTeam2().getId())
                .team1Name(match.getTeam1().getName())
                .team2Name(match.getTeam2().getName())
                .team1Flag(match.getTeam1().getFlagUrl())
                .team2Flag(match.getTeam2().getFlagUrl())
                .matchDateTime(match.getMatchDateTime())
                .venue(match.getVenue())
                .stage(match.getStage().name())
                .group(match.getGroup())
                .team1Score(match.getTeam1Score())
                .team2Score(match.getTeam2Score())
                .status(match.getStatus().name())
                .predictionLocked(match.getMatchDateTime().isBefore(LocalDateTime.now()))
                .build();
    }
}
