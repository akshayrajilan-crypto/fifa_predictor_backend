package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.Team;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnockoutAdvancementService {

    private final MatchRepository matchRepository;

    public record BracketEntry(Long targetMatchId, String slot) {}

    private static final Map<Long, BracketEntry> BRACKET_MAPPING = Map.ofEntries(
            // R32 -> R16
            Map.entry(73L, new BracketEntry(90L, "team1")),
            Map.entry(76L, new BracketEntry(90L, "team2")),
            Map.entry(75L, new BracketEntry(89L, "team1")),
            Map.entry(77L, new BracketEntry(89L, "team2")),
            Map.entry(74L, new BracketEntry(91L, "team1")),
            Map.entry(78L, new BracketEntry(91L, "team2")),
            Map.entry(79L, new BracketEntry(92L, "team1")),
            Map.entry(80L, new BracketEntry(92L, "team2")),
            Map.entry(84L, new BracketEntry(93L, "team1")),
            Map.entry(83L, new BracketEntry(93L, "team2")),
            Map.entry(81L, new BracketEntry(94L, "team1")),
            Map.entry(82L, new BracketEntry(94L, "team2")),
            Map.entry(87L, new BracketEntry(95L, "team1")),
            Map.entry(86L, new BracketEntry(95L, "team2")),
            Map.entry(85L, new BracketEntry(96L, "team1")),
            Map.entry(88L, new BracketEntry(96L, "team2")),
            // R16 -> QF
            Map.entry(89L, new BracketEntry(97L, "team1")),
            Map.entry(90L, new BracketEntry(97L, "team2")),
            Map.entry(91L, new BracketEntry(98L, "team1")),
            Map.entry(92L, new BracketEntry(98L, "team2")),
            Map.entry(93L, new BracketEntry(99L, "team1")),
            Map.entry(94L, new BracketEntry(99L, "team2")),
            Map.entry(95L, new BracketEntry(100L, "team1")),
            Map.entry(96L, new BracketEntry(100L, "team2")),
            // QF -> SF
            Map.entry(97L, new BracketEntry(101L, "team1")),
            Map.entry(98L, new BracketEntry(101L, "team2")),
            Map.entry(99L, new BracketEntry(102L, "team1")),
            Map.entry(100L, new BracketEntry(102L, "team2")),
            // SF -> Final
            Map.entry(101L, new BracketEntry(104L, "team1")),
            Map.entry(102L, new BracketEntry(104L, "team2"))
    );

    /**
     * Determines the winner of a match based on scores.
     * Returns team1 if team1Score > team2Score, team2 if team2Score > team1Score.
     * If scores are equal (draw after extra time), checks penalty scores.
     * Returns null only if no winner can be determined.
     */
    public Team determineWinner(Match match) {
        if (match.getTeam1Score() > match.getTeam2Score()) {
            return match.getTeam1();
        } else if (match.getTeam2Score() > match.getTeam1Score()) {
            return match.getTeam2();
        }
        // Scores are equal — check penalty shootout scores
        if (match.getTeam1PenaltyScore() != null && match.getTeam2PenaltyScore() != null) {
            if (match.getTeam1PenaltyScore() > match.getTeam2PenaltyScore()) {
                return match.getTeam1();
            } else if (match.getTeam2PenaltyScore() > match.getTeam1PenaltyScore()) {
                return match.getTeam2();
            }
        }
        return null;
    }

    /**
     * Advances the winner of a completed knockout match to the next round.
     * Looks up the bracket mapping to find the target match and slot, then sets the winner.
     */
    public void advanceWinner(Match completedMatch) {
        BracketEntry entry = BRACKET_MAPPING.get(completedMatch.getId());
        if (entry == null) {
            return;
        }

        Team winner = determineWinner(completedMatch);
        if (winner == null) {
            log.warn("Match {} ended in a draw - cannot advance winner", completedMatch.getId());
            return;
        }

        Long targetMatchId = entry.targetMatchId();
        Match targetMatch = matchRepository.findById(targetMatchId)
                .orElseThrow(() -> new RuntimeException("Target knockout match not found: " + targetMatchId));

        if ("team1".equals(entry.slot())) {
            targetMatch.setTeam1(winner);
        } else {
            targetMatch.setTeam2(winner);
        }

        matchRepository.save(targetMatch);
    }
}
