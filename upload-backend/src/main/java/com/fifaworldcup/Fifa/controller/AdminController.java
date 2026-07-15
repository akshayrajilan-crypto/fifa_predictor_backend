package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.InviteCodeResponse;
import com.fifaworldcup.Fifa.dto.MatchGoalScorersRequest;
import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.GoldenBallPredictionRepository;
import com.fifaworldcup.Fifa.repository.GoldenGlovePredictionRepository;
import com.fifaworldcup.Fifa.repository.InviteCodeRepository;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.repository.TopScorerPredictionRepository;
import com.fifaworldcup.Fifa.repository.UserRepository;
import com.fifaworldcup.Fifa.repository.WorldCupWinnerPredictionRepository;
import com.fifaworldcup.Fifa.service.AdminService;
import com.fifaworldcup.Fifa.service.FootballDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FootballDataService footballDataService;
    private final MatchRepository matchRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final GoldenBallPredictionRepository goldenBallPredictionRepository;
    private final GoldenGlovePredictionRepository goldenGlovePredictionRepository;
    private final WorldCupWinnerPredictionRepository worldCupWinnerPredictionRepository;
    private final UserRepository userRepository;
    private final com.fifaworldcup.Fifa.service.TournamentSettingsService tournamentSettingsService;
    private final com.fifaworldcup.Fifa.service.ApiFootballService apiFootballService;
    private final com.fifaworldcup.Fifa.repository.MatchGoalScorerRepository matchGoalScorerRepository;

    private static final int TOP_SCORER_BONUS = 4;
    private static final int GOLDEN_BALL_BONUS = 4;
    private static final int GOLDEN_GLOVE_BONUS = 4;
    private static final int WORLD_CUP_WINNER_BONUS = 5;

    @PostMapping("/match/{matchId}/omit")
    public ResponseEntity<String> toggleOmitMatch(@PathVariable Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        match.setOmitted(!match.isOmitted());
        matchRepository.save(match);
        String status = match.isOmitted() ? "omitted" : "restored";
        return ResponseEntity.ok(match.getTeam1().getName() + " vs " + match.getTeam2().getName() + " marked as " + status);
    }

    @PostMapping("/match/{matchId}/prize")
    public ResponseEntity<String> togglePrizeMatch(@PathVariable Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        match.setPrizeMatch(!match.isPrizeMatch());
        matchRepository.save(match);
        return ResponseEntity.ok(match.getTeam1().getName() + " vs " + match.getTeam2().getName() + (match.isPrizeMatch() ? " marked as prize match" : " removed from prize matches"));
    }

    @PostMapping("/match/{matchId}/prize-winner")
    public ResponseEntity<String> setPrizeWinner(@PathVariable Long matchId, @RequestParam String winner) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        match.setPrizeMatch(true);
        match.setPrizeWinner(winner);
        matchRepository.save(match);
        return ResponseEntity.ok("Prize winner set: " + winner + " for " + match.getTeam1().getName() + " vs " + match.getTeam2().getName());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changeUserPassword(@RequestParam String username, @RequestParam String newPassword) {
        com.fifaworldcup.Fifa.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Password changed for " + username);
    }

    @PostMapping("/edit-match-score")
    public ResponseEntity<String> editMatchScore(
            @RequestParam Long matchId,
            @RequestParam int team1Score,
            @RequestParam int team2Score,
            @RequestParam(required = false) Integer team1PenaltyScore,
            @RequestParam(required = false) Integer team2PenaltyScore) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        match.setTeam1Score(team1Score);
        match.setTeam2Score(team2Score);
        match.setStatus(Match.MatchStatus.COMPLETED);
        if (team1PenaltyScore != null && team2PenaltyScore != null) {
            match.setTeam1PenaltyScore(team1PenaltyScore);
            match.setTeam2PenaltyScore(team2PenaltyScore);
        }
        matchRepository.save(match);
        if (match.getStage() != Match.Stage.GROUP) {
            adminService.advanceWinnerForMatch(match);
        }
        return ResponseEntity.ok("Score updated: " + match.getTeam1().getName() + " " + team1Score + " - " + team2Score + " " + match.getTeam2().getName());
    }

    @PostMapping("/edit-match-details")
    public ResponseEntity<String> editMatchDetails(@RequestBody java.util.Map<String, Object> body) {
        Long matchId = ((Number) body.get("matchId")).longValue();
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (body.containsKey("team1Id") && body.get("team1Id") != null) {
            Long team1Id = ((Number) body.get("team1Id")).longValue();
            com.fifaworldcup.Fifa.model.Team team1 = new com.fifaworldcup.Fifa.model.Team();
            team1.setId(team1Id);
            match.setTeam1(team1);
        }
        if (body.containsKey("team2Id") && body.get("team2Id") != null) {
            Long team2Id = ((Number) body.get("team2Id")).longValue();
            com.fifaworldcup.Fifa.model.Team team2 = new com.fifaworldcup.Fifa.model.Team();
            team2.setId(team2Id);
            match.setTeam2(team2);
        }
        if (body.containsKey("matchDateTime") && body.get("matchDateTime") != null) {
            match.setMatchDateTime(java.time.LocalDateTime.parse((String) body.get("matchDateTime")));
        }
        if (body.containsKey("venue") && body.get("venue") != null) {
            match.setVenue((String) body.get("venue"));
        }

        matchRepository.save(match);
        return ResponseEntity.ok("Match details updated");
    }

    @PostMapping("/pull-results")
    public ResponseEntity<String> pullResultsFromAPI() {
        // Find all matches that should have ended (kickoff 2+ hours ago) and are not completed
        // Match times are stored in ET
        LocalDateTime twoHoursAgoET = LocalDateTime.now(java.time.ZoneId.of("America/New_York")).minusHours(2);
        List<Match> pendingMatches = matchRepository.findAllByOrderByMatchDateTimeAsc().stream()
                .filter(m -> m.getStatus() != Match.MatchStatus.COMPLETED)
                .filter(m -> m.getMatchDateTime().isBefore(twoHoursAgoET))
                .toList();

        if (pendingMatches.isEmpty()) {
            return ResponseEntity.ok("No pending matches to update. All finished matches are already recorded.");
        }

        int updated = 0;
        for (Match match : pendingMatches) {
            try {
                footballDataService.fetchAndUpdateMatchResult(match.getId());
                // Re-check if it was updated
                Match refreshed = matchRepository.findById(match.getId()).orElse(match);
                if (refreshed.getStatus() == Match.MatchStatus.COMPLETED) {
                    updated++;
                }
            } catch (Exception e) {
                // Continue with next match
            }
        }

        return ResponseEntity.ok("Pulled results: " + updated + " match(es) updated out of " + pendingMatches.size() + " pending.");
    }

    @PostMapping("/pull-result/{matchId}")
    public ResponseEntity<String> pullSingleMatchResult(@PathVariable Long matchId) {
        try {
            // Force re-pull even if already completed (reset status temporarily)
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found"));

            Match.MatchStatus originalStatus = match.getStatus();
            if (originalStatus == Match.MatchStatus.COMPLETED) {
                // Temporarily set to UPCOMING so the service processes it
                match.setStatus(Match.MatchStatus.UPCOMING);
                match.setTeam1Score(null);
                match.setTeam2Score(null);
                matchRepository.save(match);
            }

            footballDataService.fetchAndUpdateMatchResult(matchId);

            match = matchRepository.findById(matchId).orElse(match);
            if (match.getStatus() == Match.MatchStatus.COMPLETED) {
                return ResponseEntity.ok("Match result pulled: " +
                        match.getTeam1().getName() + " " + match.getTeam1Score() +
                        " - " + match.getTeam2Score() + " " + match.getTeam2().getName());
            } else {
                // Restore original status if pull failed
                match.setStatus(originalStatus);
                matchRepository.save(match);
                return ResponseEntity.ok("Match result not available from API yet. Try again later.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/match-result")
    public ResponseEntity<String> submitMatchResult(@Valid @RequestBody MatchResultRequest request) {
        adminService.submitMatchResult(request);
        return ResponseEntity.ok("Match result submitted and score prediction points calculated");
    }

    @PostMapping("/match-goal-scorers")
    public ResponseEntity<String> submitMatchGoalScorers(@Valid @RequestBody MatchGoalScorersRequest request) {
        adminService.submitMatchGoalScorers(request);
        return ResponseEntity.ok("Goal scorer points calculated");
    }

    /**
     * Fetches goal scorers from API-Football for a match (preview only, doesn't save).
     */
    @GetMapping("/match-goal-scorers/fetch/{matchId}")
    public ResponseEntity<List<java.util.Map<String, Object>>> fetchGoalScorersFromApi(@PathVariable Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        List<java.util.Map<String, Object>> scorers = apiFootballService.fetchGoalScorersFromApi(match);
        return ResponseEntity.ok(scorers);
    }

    /**
     * Saves (or updates) goal scorers for a match. Replaces existing data.
     * Accepts a list of scorer objects: [{playerName, minute, ownGoal, penalty}]
     */
    @PostMapping("/match-goal-scorers/save/{matchId}")
    public ResponseEntity<String> saveMatchGoalScorers(
            @PathVariable Long matchId,
            @RequestBody List<java.util.Map<String, Object>> scorers) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        apiFootballService.saveGoalScorers(match, scorers);
        return ResponseEntity.ok("Goal scorers saved for " + match.getTeam1().getName() + " vs " + match.getTeam2().getName());
    }

    /**
     * Gets existing goal scorers for a match (for editing in admin).
     */
    @GetMapping("/match-goal-scorers/{matchId}")
    public ResponseEntity<List<java.util.Map<String, Object>>> getMatchGoalScorers(@PathVariable Long matchId) {
        var scorers = matchGoalScorerRepository.findByMatchId(matchId);
        var result = scorers.stream().map(s -> {
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("playerName", s.getPlayerName());
            map.put("minute", s.getMinute());
            map.put("ownGoal", s.isOwnGoal());
            map.put("penalty", s.isPenalty());
            map.put("firstGoal", s.isFirstGoal());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/submit-motm")
    public ResponseEntity<String> submitMotm(@RequestParam Long matchId, @RequestParam String playerName) {
        adminService.submitMotm(matchId, playerName);
        return ResponseEntity.ok("Man of the Match set and points awarded");
    }

    @PostMapping("/recalculate-points")
    public ResponseEntity<String> recalculateAllPoints() {
        String result = adminService.recalculateAllPoints();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-predictions/{username}")
    public ResponseEntity<java.util.Map<String, Object>> getUserPredictions(@PathVariable String username) {
        return ResponseEntity.ok(adminService.getUserPredictionsForAdmin(username));
    }

    @PostMapping("/update-prediction-points")
    public ResponseEntity<String> updatePredictionPoints(
            @RequestParam String type,
            @RequestParam Long predictionId,
            @RequestParam int points) {
        adminService.updatePredictionPoints(type, predictionId, points);
        return ResponseEntity.ok("Prediction points updated");
    }

    @PostMapping("/award-top-scorer")
    public ResponseEntity<String> awardTopScorer(@RequestParam String playerName) {
        List<TopScorerPrediction> predictions = topScorerPredictionRepository.findAll();
        int awarded = 0;
        for (TopScorerPrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(TOP_SCORER_BONUS);
                prediction.setScored(true);
                topScorerPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + TOP_SCORER_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Top scorer points awarded to " + awarded + " users");
    }

    @PostMapping("/award-golden-ball")
    public ResponseEntity<String> awardGoldenBall(@RequestParam String playerName) {
        List<GoldenBallPrediction> predictions = goldenBallPredictionRepository.findAll();
        int awarded = 0;
        for (GoldenBallPrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(GOLDEN_BALL_BONUS);
                prediction.setScored(true);
                goldenBallPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + GOLDEN_BALL_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Golden Ball points awarded to " + awarded + " users");
    }

    @PostMapping("/award-golden-glove")
    public ResponseEntity<String> awardGoldenGlove(@RequestParam String playerName) {
        List<GoldenGlovePrediction> predictions = goldenGlovePredictionRepository.findAll();
        int awarded = 0;
        for (GoldenGlovePrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(GOLDEN_GLOVE_BONUS);
                prediction.setScored(true);
                goldenGlovePredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + GOLDEN_GLOVE_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Golden Glove points awarded to " + awarded + " users");
    }

    @PostMapping("/award-world-cup-winner")
    public ResponseEntity<String> awardWorldCupWinner(@RequestParam Long teamId) {
        List<WorldCupWinnerPrediction> predictions = worldCupWinnerPredictionRepository.findAll();
        int awarded = 0;
        for (WorldCupWinnerPrediction prediction : predictions) {
            if (prediction.getTeam().getId().equals(teamId) && !prediction.isScored()) {
                prediction.setPointsEarned(WORLD_CUP_WINNER_BONUS);
                prediction.setScored(true);
                worldCupWinnerPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + WORLD_CUP_WINNER_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("World Cup Winner points awarded to " + awarded + " users");
    }

    // ─── Invite Code Management ─────────────────────────────

    @PostMapping("/invite-codes/generate")
    public ResponseEntity<InviteCodeResponse> generateInviteCode(@RequestParam(required = false) String label,
                                                                  @RequestParam(defaultValue = "1") int count) {
        if (count == 1) {
            InviteCode code = createInviteCode(label);
            return ResponseEntity.ok(toInviteResponse(code));
        }
        // For bulk, return the last one (use /invite-codes/generate-bulk for multiple)
        InviteCode code = createInviteCode(label);
        return ResponseEntity.ok(toInviteResponse(code));
    }

    @PostMapping("/invite-codes/generate-bulk")
    public ResponseEntity<java.util.List<InviteCodeResponse>> generateBulkInviteCodes(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String labelPrefix) {
        java.util.List<InviteCodeResponse> codes = new java.util.ArrayList<>();
        for (int i = 1; i <= Math.min(count, 50); i++) {
            String lbl = labelPrefix != null ? labelPrefix + " #" + i : null;
            InviteCode code = createInviteCode(lbl);
            codes.add(toInviteResponse(code));
        }
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/invite-codes")
    public ResponseEntity<java.util.List<InviteCodeResponse>> getAllInviteCodes() {
        return ResponseEntity.ok(
            inviteCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toInviteResponse)
                .toList()
        );
    }

    @DeleteMapping("/invite-codes/{id}")
    public ResponseEntity<String> deleteInviteCode(@PathVariable Long id) {
        InviteCode code = inviteCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invite code not found"));
        if (code.isUsed()) {
            return ResponseEntity.badRequest().body("Cannot delete a used invite code");
        }
        inviteCodeRepository.delete(code);
        return ResponseEntity.ok("Invite code deleted");
    }

    private InviteCode createInviteCode(String label) {
        String code = generateCode();
        InviteCode inviteCode = InviteCode.builder()
                .code(code)
                .label(label)
                .build();
        return inviteCodeRepository.save(inviteCode);
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";  // no I, O, 0, 1 to avoid confusion
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private InviteCodeResponse toInviteResponse(InviteCode code) {
        return InviteCodeResponse.builder()
                .id(code.getId())
                .code(code.getCode())
                .label(code.getLabel())
                .used(code.isUsed())
                .usedByUsername(code.getUsedByUsername())
                .createdAt(code.getCreatedAt())
                .usedAt(code.getUsedAt())
                .build();
    }

    // ─── Tournament Prediction Lock Settings ───────────────────────────

    @GetMapping("/tournament-settings")
    public ResponseEntity<?> getTournamentSettings() {
        var settings = tournamentSettingsService.getSettings();
        return ResponseEntity.ok(java.util.Map.of(
                "tournamentPredictionLockTime", settings.getTournamentPredictionLockTime() != null ? settings.getTournamentPredictionLockTime().toString() : "",
                "tournamentPredictionsLocked", settings.isTournamentPredictionsLocked(),
                "isCurrentlyLocked", tournamentSettingsService.areTournamentPredictionsLocked()
        ));
    }

    @PostMapping("/tournament-settings/set-lock-time")
    public ResponseEntity<?> setTournamentLockTime(@RequestParam String lockTime) {
        LocalDateTime parsedTime = LocalDateTime.parse(lockTime);
        var settings = tournamentSettingsService.setLockTime(parsedTime);
        return ResponseEntity.ok(java.util.Map.of(
                "message", "Tournament prediction lock time set to: " + parsedTime,
                "tournamentPredictionLockTime", settings.getTournamentPredictionLockTime().toString()
        ));
    }

    @PostMapping("/tournament-settings/lock-now")
    public ResponseEntity<?> lockTournamentPredictions() {
        tournamentSettingsService.lockNow();
        return ResponseEntity.ok(java.util.Map.of("message", "Tournament predictions locked manually."));
    }

    @PostMapping("/tournament-settings/unlock")
    public ResponseEntity<?> unlockTournamentPredictions() {
        tournamentSettingsService.unlock();
        return ResponseEntity.ok(java.util.Map.of("message", "Tournament predictions unlocked."));
    }

    /**
     * Ensures QF/SF/Final placeholder matches exist and sets known R16 teams.
     * Safe to call multiple times — idempotent.
     */
    @PostMapping("/setup-bracket")
    public ResponseEntity<String> setupBracket() {
        String result = adminService.setupBracket();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fix-bracket")
    public ResponseEntity<String> fixCompletedBracket() {
        String result = adminService.fixCompletedBracket();
        return ResponseEntity.ok(result);
    }
}
