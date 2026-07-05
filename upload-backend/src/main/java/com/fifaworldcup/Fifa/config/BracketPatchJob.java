package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Patches the live DB bracket on every startup without requiring direct DB access.
 * - Ensures QF/SF/3rd place/Final placeholder matches exist with correct IDs.
 * - Sets known R16 team slots (Argentina vs Egypt, Portugal vs Spain).
 * Idempotent: skips anything already set.
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class BracketPatchJob implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    @Override
    public void run(String... args) {
        // Ensure QF/SF/Final placeholder matches exist (live DB ran old seeder without them)
        ensurePlaceholder(97L,  "2026-07-09T16:00", "Dallas Stadium (AT&T)",                 Match.Stage.QUARTER_FINAL);
        ensurePlaceholder(98L,  "2026-07-09T20:00", "Los Angeles Stadium (SoFi)",            Match.Stage.QUARTER_FINAL);
        ensurePlaceholder(99L,  "2026-07-10T16:00", "New York New Jersey Stadium (MetLife)", Match.Stage.QUARTER_FINAL);
        ensurePlaceholder(100L, "2026-07-11T16:00", "Miami Stadium (Hard Rock)",             Match.Stage.QUARTER_FINAL);
        ensurePlaceholder(101L, "2026-07-14T20:00", "Dallas Stadium (AT&T)",                 Match.Stage.SEMI_FINAL);
        ensurePlaceholder(102L, "2026-07-15T20:00", "New York New Jersey Stadium (MetLife)", Match.Stage.SEMI_FINAL);
        ensurePlaceholder(103L, "2026-07-18T16:00", "Miami Stadium (Hard Rock)",             Match.Stage.THIRD_PLACE);
        ensurePlaceholder(104L, "2026-07-19T20:00", "New York New Jersey Stadium (MetLife)", Match.Stage.FINAL);

        // M95: Winner M87 (Argentina) vs Winner M86 (Egypt)
        setTeam(95L, "team1", "Argentina");
        setTeam(95L, "team2", "Egypt");

        // M93: Winner M84 (Portugal) vs Winner M83 (Spain)
        setTeam(93L, "team1", "Portugal");
        setTeam(93L, "team2", "Spain");

        log.info("✅ BracketPatchJob complete.");
    }

    /** Sets a team slot on a match only if that slot is currently null. */
    private void setTeam(Long matchId, String slot, String teamName) {
        matchRepository.findById(matchId).ifPresent(match -> {
            if ("team1".equals(slot) && match.getTeam1() != null) return;
            if ("team2".equals(slot) && match.getTeam2() != null) return;

            teamRepository.findByName(teamName).ifPresentOrElse(team -> {
                if ("team1".equals(slot)) match.setTeam1(team);
                else match.setTeam2(team);
                matchRepository.save(match);
                log.info("✅ Set match {} {} = {}", matchId, slot, teamName);
            }, () -> log.warn("⚠️  Team not found: {}", teamName));
        });
    }

    /**
     * Creates a placeholder match at the given ID only if it doesn't already exist.
     * Uses a native INSERT to preserve the specific ID required by BRACKET_MAPPING.
     */
    @Transactional
    private void ensurePlaceholder(Long id, String dateTime, String venue, Match.Stage stage) {
        if (matchRepository.existsById(id)) return;
        matchRepository.saveWithId(id, LocalDateTime.parse(dateTime), venue, stage.name());
        log.info("✅ Created placeholder match {} ({})", id, stage);
    }
}
