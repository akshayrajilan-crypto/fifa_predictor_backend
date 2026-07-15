package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.GroupStandingsResponse;
import com.fifaworldcup.Fifa.dto.MatchResponse;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchGoalScorerRepository;
import com.fifaworldcup.Fifa.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchGoalScorerRepository matchGoalScorerRepository;
    private final com.fifaworldcup.Fifa.repository.TeamRepository teamRepository;

    @GetMapping
    public ResponseEntity<List<MatchResponse>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> getAllTeams() {
        return ResponseEntity.ok(teamRepository.findAll().stream().map(t -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("group", t.getGroup());
            return m;
        }).toList());
    }

    @GetMapping("/prize-winners")
    public ResponseEntity<List<Map<String, Object>>> getPrizeWinners() {
        List<Match> prizeMatches = matchRepository.findAll().stream()
                .filter(Match::isPrizeMatch)
                .filter(m -> m.getPrizeWinner() != null && !m.getPrizeWinner().isBlank())
                .toList();
        return ResponseEntity.ok(prizeMatches.stream().map(m -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("matchId", m.getId());
            map.put("team1", m.getTeam1() != null ? m.getTeam1().getName() : "TBD");
            map.put("team2", m.getTeam2() != null ? m.getTeam2().getName() : "TBD");
            map.put("team1Score", m.getTeam1Score());
            map.put("team2Score", m.getTeam2Score());
            map.put("winner", m.getPrizeWinner());
            map.put("stage", m.getStage().name());
            return map;
        }).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @GetMapping("/{id}/scorers")
    public ResponseEntity<List<Map<String, Object>>> getMatchScorers(@PathVariable Long id) {
        var scorers = matchGoalScorerRepository.findByMatchId(id);
        var result = scorers.stream().map(s -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("playerName", s.getPlayerName());
            map.put("minute", s.getMinute());
            map.put("isFirstGoal", s.isFirstGoal());
            map.put("isOwnGoal", s.isOwnGoal());
            map.put("isPenalty", s.isPenalty());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/today")
    public ResponseEntity<List<MatchResponse>> getTodaysMatches() {
        return ResponseEntity.ok(matchService.getTodaysMatches());
    }

    @GetMapping("/stage/{stage}")
    public ResponseEntity<List<MatchResponse>> getByStage(@PathVariable Match.Stage stage) {
        return ResponseEntity.ok(matchService.getMatchesByStage(stage));
    }

    @GetMapping("/group/{group}")
    public ResponseEntity<List<MatchResponse>> getByGroup(@PathVariable String group) {
        return ResponseEntity.ok(matchService.getMatchesByGroup(group));
    }

    @GetMapping("/standings")
    public ResponseEntity<Map<String, List<GroupStandingsResponse>>> getAllStandings() {
        return ResponseEntity.ok(matchService.computeAllStandings());
    }

    @GetMapping("/standings/{group}")
    public ResponseEntity<List<GroupStandingsResponse>> getStandingsByGroup(@PathVariable String group) {
        return ResponseEntity.ok(matchService.computeGroupStandings(group));
    }
}
