package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final MatchGoalScorerRepository matchGoalScorerRepository;
    private final UserRepository userRepository;
    private final KnockoutAdvancementService knockoutAdvancementService;

    private static final int MATCH_WINNER_POINTS = 1;       // Correct result (win/draw)
    private static final int EXACT_SCORE_POINTS = 3;        // Exact score (bonus on top of match winner)
    private static final int GOAL_SCORER_POINTS = 3;        // Per correct goal scorer
    private static final int WRONG_SCORER_PENALTY = -1;     // Per wrong goal scorer prediction
    private static final int MOTM_POINTS = 3;               // Correct man of the match
    private static final int TOP_SCORER_POINTS = 4;         // Tournament top scorer
    private static final int GOLDEN_BALL_POINTS = 4;        // Tournament golden ball
    private static final int GOLDEN_GLOVE_POINTS = 4;       // Tournament golden glove
    private static final int WORLD_CUP_WINNER_POINTS = 5;   // Correct world cup winner

    @Transactional
    public void submitMatchResult(MatchResultRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setTeam1Score(request.getTeam1Score());
        match.setTeam2Score(request.getTeam2Score());
        match.setStatus(Match.MatchStatus.COMPLETED);
        matchRepository.save(match);

        // Advance winner to next round if this is a knockout match
        if (match.getStage() != Match.Stage.GROUP) {
            knockoutAdvancementService.advanceWinner(match);
        }

        calculateScorePoints(match);
    }

    @Transactional
    public void submitMotm(Long matchId, String playerName) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setManOfTheMatch(playerName);
        matchRepository.save(match);

        // Award MOTM points
        List<MotmPrediction> predictions = motmPredictionRepository.findByMatchAndScored(match, false);
        for (MotmPrediction prediction : predictions) {
            String predictedName = prediction.getPlayer().getName().toLowerCase();
            if (namesMatch(predictedName, playerName.toLowerCase())) {
                prediction.setPointsEarned(MOTM_POINTS);
            }
            prediction.setScored(true);
            motmPredictionRepository.save(prediction);

            if (prediction.getPointsEarned() > 0) {
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + prediction.getPointsEarned());
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void submitMatchGoalScorers(com.fifaworldcup.Fifa.dto.MatchGoalScorersRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != Match.MatchStatus.COMPLETED) {
            throw new RuntimeException("Submit match result first before adding goal scorers");
        }

        // Get actual scorer player IDs and count occurrences (for multi-goal scoring)
        java.util.Map<Long, Integer> actualGoalCounts = new java.util.HashMap<>();
        for (Long scorerId : request.getScorerPlayerIds()) {
            actualGoalCounts.merge(scorerId, 1, Integer::sum);
        }

        List<GoalScorerPrediction> predictions = goalScorerPredictionRepository.findByMatchAndScored(match, false);

        for (GoalScorerPrediction prediction : predictions) {
            int points = 0;
            Long predictedPlayerId = prediction.getPlayer().getId();
            int predictedGoals = prediction.getPredictedGoals();

            if (actualGoalCounts.containsKey(predictedPlayerId)) {
                int actualGoals = actualGoalCounts.get(predictedPlayerId);
                // Award points for correct goals
                int goalsToReward = Math.min(actualGoals, predictedGoals);
                points = GOAL_SCORER_POINTS * goalsToReward;
                // Penalize over-predicted goals (predicted more than actual)
                int wrongGoals = predictedGoals - actualGoals;
                if (wrongGoals > 0) {
                    points += WRONG_SCORER_PENALTY * wrongGoals;
                }
            } else {
                // Player didn't score at all — penalize all predicted goals
                points = WRONG_SCORER_PENALTY * predictedGoals;
            }

            prediction.setPointsEarned(points);
            prediction.setScored(true);
            goalScorerPredictionRepository.save(prediction);

            if (points != 0) {
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + points);
                userRepository.save(user);
            }
        }
    }

    private void calculateScorePoints(Match match) {
        List<Prediction> predictions = predictionRepository.findByMatchAndScored(match, false);

        for (Prediction prediction : predictions) {
            int points = calculatePredictionPoints(prediction, match);
            prediction.setPointsEarned(points);
            prediction.setScored(true);
            predictionRepository.save(prediction);

            User user = prediction.getUser();
            user.setTotalPoints(user.getTotalPoints() + points);
            userRepository.save(user);
        }
    }

    private int calculatePredictionPoints(Prediction prediction, Match match) {
        int actualTeam1 = match.getTeam1Score();
        int actualTeam2 = match.getTeam2Score();
        int predictedTeam1 = prediction.getPredictedTeam1Score();
        int predictedTeam2 = prediction.getPredictedTeam2Score();

        int points = 0;

        // Check result (win/draw)
        String actualResult = getResult(actualTeam1, actualTeam2);
        String predictedResult = getResult(predictedTeam1, predictedTeam2);

        if (actualResult.equals(predictedResult)) {
            points += MATCH_WINNER_POINTS;  // +1 for correct result

            // Exact score bonus
            if (predictedTeam1 == actualTeam1 && predictedTeam2 == actualTeam2) {
                points += EXACT_SCORE_POINTS;  // +2 for exact score (total +3)
            }
        }

        return points;
    }

    private String getResult(int score1, int score2) {
        if (score1 > score2) return "WIN1";
        if (score1 < score2) return "WIN2";
        return "DRAW";
    }

    private boolean namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String a = java.text.Normalizer.normalize(name1.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String b = java.text.Normalizer.normalize(name2.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        if (a.equals(b)) return true;
        // Check last name match
        String[] partsA = a.split("\\s+");
        String[] partsB = b.split("\\s+");
        String lastA = partsA[partsA.length - 1];
        String lastB = partsB[partsB.length - 1];
        return lastA.equals(lastB) && lastA.length() > 3;
    }

    /**
     * Ensures QF/SF/3rd/Final placeholder matches exist (IDs 97-104)
     * and sets known R16 teams (Argentina/Egypt on M95, Portugal/Spain on M93).
     * Idempotent — safe to call multiple times.
     */
    @Transactional
    public String setupBracket() {
        StringBuilder log = new StringBuilder();

        Object[][] placeholders = {
            {97L,  "2026-07-11T14:00", "Dallas Stadium (AT&T)",                   "QUARTER_FINAL"},
            {98L,  "2026-07-11T18:00", "Atlanta Stadium (Mercedes-Benz)",          "QUARTER_FINAL"},
            {99L,  "2026-07-12T14:00", "Los Angeles Stadium (SoFi)",              "QUARTER_FINAL"},
            {100L, "2026-07-12T18:00", "New York New Jersey Stadium (MetLife)",   "QUARTER_FINAL"},
            {101L, "2026-07-15T15:00", "Houston Stadium (NRG)",                   "SEMI_FINAL"},
            {102L, "2026-07-16T15:00", "San Francisco Bay Area Stadium (Levi's)", "SEMI_FINAL"},
            {103L, "2026-07-19T15:00", "Miami Stadium (Hard Rock)",               "THIRD_PLACE"},
            {104L, "2026-07-19T19:00", "MetLife Stadium (New York)",              "FINAL"},
        };

        for (Object[] p : placeholders) {
            Long id = (Long) p[0];
            if (matchRepository.findById(id).isEmpty()) {
                matchRepository.save(Match.builder()
                        .matchDateTime(java.time.LocalDateTime.parse((String) p[1]))
                        .venue((String) p[2])
                        .stage(Match.Stage.valueOf((String) p[3]))
                        .status(Match.MatchStatus.UPCOMING)
                        .build());
                log.append("Created placeholder match ").append(id).append("\n");
            } else {
                log.append("Match ").append(id).append(" already exists\n");
            }
        }

        setTeamOnMatch(95L, "Argentina", true, log);
        setTeamOnMatch(95L, "Egypt",     false, log);
        setTeamOnMatch(93L, "Portugal",  true, log);
        setTeamOnMatch(93L, "Spain",     false, log);

        return log.toString();
    }

    private void setTeamOnMatch(Long matchId, String teamName, boolean isTeam1, StringBuilder log) {
        matchRepository.findById(matchId).ifPresent(m -> {
            if (isTeam1 ? m.getTeam1() != null : m.getTeam2() != null) {
                log.append(teamName).append(" already set on M").append(matchId).append("\n");
                return;
            }
            teamRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase(teamName))
                .findFirst()
                .ifPresent(t -> {
                    if (isTeam1) m.setTeam1(t); else m.setTeam2(t);
                    matchRepository.save(m);
                    log.append("Set ").append(teamName).append(" on M").append(matchId).append("\n");
                });
        });
    }

    public java.util.Map<String, Object> getUserPredictionsForAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("username", user.getUsername());
        result.put("totalPoints", user.getTotalPoints());

        // Match predictions
        List<Prediction> predictions = predictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> matchPreds = new java.util.ArrayList<>();
        for (Prediction p : predictions) {
            if (p.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("match", p.getMatch().getTeam1().getName() + " vs " + p.getMatch().getTeam2().getName());
                m.put("predicted", p.getPredictedTeam1Score() + "-" + p.getPredictedTeam2Score());
                m.put("actual", p.getMatch().getTeam1Score() + "-" + p.getMatch().getTeam2Score());
                m.put("points", p.getPointsEarned());
                matchPreds.add(m);
            }
        }
        result.put("matchPredictions", matchPreds);

        // Goal scorer predictions
        List<GoalScorerPrediction> gsPreds = goalScorerPredictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> gsList = new java.util.ArrayList<>();
        for (GoalScorerPrediction gs : gsPreds) {
            if (gs.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", gs.getId());
                m.put("match", gs.getMatch().getTeam1().getName() + " vs " + gs.getMatch().getTeam2().getName());
                m.put("player", gs.getPlayer().getName());
                m.put("predictedGoals", gs.getPredictedGoals());
                m.put("points", gs.getPointsEarned());
                gsList.add(m);
            }
        }
        result.put("goalScorerPredictions", gsList);

        // MOTM predictions
        List<MotmPrediction> motmPreds = motmPredictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> motmList = new java.util.ArrayList<>();
        for (MotmPrediction mp : motmPreds) {
            if (mp.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", mp.getId());
                m.put("match", mp.getMatch().getTeam1().getName() + " vs " + mp.getMatch().getTeam2().getName());
                m.put("player", mp.getPlayer().getName());
                m.put("points", mp.getPointsEarned());
                motmList.add(m);
            }
        }
        result.put("motmPredictions", motmList);

        return result;
    }

    /**
     * Updates points on a specific prediction by type and ID.
     * Also updates the user's total points accordingly.
     */
    @Transactional
    public void updatePredictionPoints(String type, Long predictionId, int newPoints) {
        switch (type) {
            case "match" -> {
                Prediction p = predictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                predictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            case "goalScorer" -> {
                GoalScorerPrediction p = goalScorerPredictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Goal scorer prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                goalScorerPredictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            case "motm" -> {
                MotmPrediction p = motmPredictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("MOTM prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                motmPredictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            default -> throw new RuntimeException("Unknown prediction type: " + type);
        }
    }

    /**
     * Recalculates ALL points from scratch for every user.
     * Resets all user points to 0, then re-scores every completed match.
     */
    @Transactional
    public String recalculateAllPoints() {
        // Reset all user points to 0
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.setTotalPoints(0);
            userRepository.save(user);
        }

        // Reset all prediction scores
        List<Prediction> allPreds = predictionRepository.findAll();
        for (Prediction p : allPreds) {
            p.setPointsEarned(0);
            p.setScored(false);
            predictionRepository.save(p);
        }

        List<GoalScorerPrediction> allGs = goalScorerPredictionRepository.findAll();
        for (GoalScorerPrediction p : allGs) {
            p.setPointsEarned(0);
            p.setScored(false);
            goalScorerPredictionRepository.save(p);
        }

        List<MotmPrediction> allMotm = motmPredictionRepository.findAll();
        for (MotmPrediction p : allMotm) {
            p.setPointsEarned(0);
            p.setScored(false);
            motmPredictionRepository.save(p);
        }

        // Re-score all completed matches (skip omitted)
        List<Match> completedMatches = matchRepository.findByStatus(Match.MatchStatus.COMPLETED);
        int matchesScored = 0;

        for (Match match : completedMatches) {
            // Skip omitted matches
            if (match.isOmitted()) continue;
            // Score predictions
            calculateScorePoints(match);

            // Score goal scorers using actual scorer data from match_goal_scorers table
            List<MatchGoalScorer> actualScorers = matchGoalScorerRepository.findByMatchOrderByMinuteAsc(match);
            if (!actualScorers.isEmpty()) {
                // Count actual goals per player name (exclude own goals)
                java.util.Map<String, Integer> actualGoalsByName = new java.util.HashMap<>();
                for (MatchGoalScorer gs : actualScorers) {
                    if (!gs.isOwnGoal()) {
                        actualGoalsByName.merge(gs.getPlayerName().toLowerCase(), 1, Integer::sum);
                    }
                }

                List<GoalScorerPrediction> gsPreds = goalScorerPredictionRepository.findByMatchAndScored(match, false);
                for (GoalScorerPrediction prediction : gsPreds) {
                    int points = 0;
                    String predictedPlayerName = prediction.getPlayer().getName().toLowerCase();
                    int predictedGoals = prediction.getPredictedGoals();

                    // Fuzzy match against actual scorers
                    int actualGoals = 0;
                    for (java.util.Map.Entry<String, Integer> entry : actualGoalsByName.entrySet()) {
                        if (namesMatch(predictedPlayerName, entry.getKey())) {
                            actualGoals = entry.getValue();
                            break;
                        }
                    }

                    if (actualGoals > 0) {
                        int correctGoals = Math.min(actualGoals, predictedGoals);
                        int wrongGoals = predictedGoals - correctGoals;
                        points = (GOAL_SCORER_POINTS * correctGoals) + (WRONG_SCORER_PENALTY * wrongGoals);
                    } else {
                        // Player didn't score at all — penalize all predicted goals
                        points = WRONG_SCORER_PENALTY * predictedGoals;
                    }

                    prediction.setPointsEarned(points);
                    prediction.setScored(true);
                    goalScorerPredictionRepository.save(prediction);

                    if (points != 0) {
                        User user = prediction.getUser();
                        user.setTotalPoints(user.getTotalPoints() + points);
                        userRepository.save(user);
                    }
                }
            }

            // Score MOTM
            if (match.getManOfTheMatch() != null && !match.getManOfTheMatch().isBlank()) {
                List<MotmPrediction> motmPreds = motmPredictionRepository.findByMatchAndScored(match, false);
                for (MotmPrediction pred : motmPreds) {
                    String predictedName = pred.getPlayer().getName().toLowerCase();
                    if (namesMatch(predictedName, match.getManOfTheMatch().toLowerCase())) {
                        pred.setPointsEarned(MOTM_POINTS);
                    }
                    pred.setScored(true);
                    motmPredictionRepository.save(pred);

                    if (pred.getPointsEarned() > 0) {
                        User user = pred.getUser();
                        user.setTotalPoints(user.getTotalPoints() + pred.getPointsEarned());
                        userRepository.save(user);
                    }
                }
            }

            matchesScored++;
        }

        return "Recalculated points for " + matchesScored + " completed matches. All user totals reset and re-scored.";
    }
}
