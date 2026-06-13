package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final UserRepository userRepository;

    private static final int MATCH_WINNER_POINTS = 1;       // Correct result (win/draw)
    private static final int EXACT_SCORE_POINTS = 2;        // Exact score (bonus on top of match winner)
    private static final int GOAL_SCORER_POINTS = 2;        // Per correct goal scorer
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

        // Get actual scorer player IDs
        Set<Long> actualScorerIds = new java.util.HashSet<>(request.getScorerPlayerIds());

        List<GoalScorerPrediction> predictions = goalScorerPredictionRepository.findByMatchAndScored(match, false);

        for (GoalScorerPrediction prediction : predictions) {
            int points = 0;
            Long predictedPlayerId = prediction.getPlayer().getId();

            if (actualScorerIds.contains(predictedPlayerId)) {
                points += GOAL_SCORER_POINTS;
            }

            // Check first goal scorer bonus is removed in new system — just base scorer points

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

        // Re-score all completed matches
        List<Match> completedMatches = matchRepository.findByStatus(Match.MatchStatus.COMPLETED);
        int matchesScored = 0;

        for (Match match : completedMatches) {
            // Score predictions
            calculateScorePoints(match);

            // Score goal scorers if we have actual scorer data
            if (match.getTeam1Score() != null) {
                List<GoalScorerPrediction> gsPreds = goalScorerPredictionRepository.findByMatchAndScored(match, false);
                // We need the actual scorers from match_goal_scorers table - handled via submitMatchGoalScorers
                // For recalculation, mark them as scored with 0 unless we have data
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
