package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sends push notifications 30 minutes before each match starts.
 * Checks every 5 minutes for upcoming matches in the 25-35 min window.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchReminderScheduler {

    private final MatchRepository matchRepository;
    private final PushNotificationService pushNotificationService;

    // Track which matches we've already notified for (to avoid duplicates)
    private final Set<Long> notifiedMatches = new HashSet<>();

    /**
     * Runs every 5 minutes. Checks if any match starts in ~30 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void sendMatchReminders() {
        // Match times are stored in ET
        LocalDateTime nowET = LocalDateTime.now(ZoneId.of("America/New_York"));
        LocalDateTime in25min = nowET.plusMinutes(25);
        LocalDateTime in35min = nowET.plusMinutes(35);

        List<Match> upcomingMatches = matchRepository.findByMatchDateTimeBetween(in25min, in35min)
                .stream()
                .filter(m -> m.getStatus() == Match.MatchStatus.UPCOMING)
                .filter(m -> !notifiedMatches.contains(m.getId()))
                .toList();

        for (Match match : upcomingMatches) {
            String team1 = match.getTeam1() != null ? match.getTeam1().getName() : "TBD";
            String team2 = match.getTeam2() != null ? match.getTeam2().getName() : "TBD";

            String title = "⚽ Match Starting Soon!";
            String body = team1 + " vs " + team2 + " kicks off in 30 minutes! Make your prediction now.";

            pushNotificationService.sendToAll(title, body);
            notifiedMatches.add(match.getId());

            log.info("🔔 Sent reminder for {} vs {}", team1, team2);
        }
    }
}
