package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.*;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final GoldenBallPredictionRepository goldenBallPredictionRepository;
    private final GoldenGlovePredictionRepository goldenGlovePredictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final WorldCupWinnerPredictionRepository worldCupWinnerPredictionRepository;

    public PredictionResponse makePrediction(String username, PredictionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Check if prediction is locked (match has started)
        if (match.getMatchDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Predictions are locked. Match has already started.");
        }

        // Check if user already predicted this match
        Prediction prediction = predictionRepository.findByUserAndMatch(user, match)
                .orElse(Prediction.builder()
                        .user(user)
                        .match(match)
                        .build());

        prediction.setPredictedTeam1Score(request.getPredictedTeam1Score());
        prediction.setPredictedTeam2Score(request.getPredictedTeam2Score());
        prediction.setUpdatedAt(LocalDateTime.now());

        predictionRepository.save(prediction);
        return toResponse(prediction);
    }

    @Transactional
    public List<GoalScorerPredictionResponse> predictGoalScorers(String username, GoalScorerPredictionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getMatchDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Predictions are locked. Match has already started.");
        }

        // Remove existing goal scorer predictions for this user + match
        goalScorerPredictionRepository.deleteByUserAndMatch(user, match);

        // Save new predictions
        for (Long playerId : request.getPlayerIds()) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));

            boolean isFirst = request.getFirstGoalScorerPlayerId() != null
                    && request.getFirstGoalScorerPlayerId().equals(playerId);

            goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                    .user(user)
                    .match(match)
                    .player(player)
                    .isFirstGoalScorer(isFirst)
                    .build());
        }

        // If first goal scorer is not in the scorer list, add separately
        if (request.getFirstGoalScorerPlayerId() != null
                && !request.getPlayerIds().contains(request.getFirstGoalScorerPlayerId())) {
            Player firstScorer = playerRepository.findById(request.getFirstGoalScorerPlayerId())
                    .orElseThrow(() -> new RuntimeException("Player not found"));
            goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                    .user(user)
                    .match(match)
                    .player(firstScorer)
                    .isFirstGoalScorer(true)
                    .build());
        }

        return getGoalScorerPredictions(username, request.getMatchId());
    }

    public List<GoalScorerPredictionResponse> getGoalScorerPredictions(String username, Long matchId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        return goalScorerPredictionRepository.findByUserAndMatch(user, match).stream()
                .map(this::toGoalScorerResponse)
                .toList();
    }

    public List<GoalScorerPredictionResponse> getAllGoalScorerPredictionsForMatch(Long matchId, String requestingUsername) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Only show all users' predictions after match is completed
        if (match.getStatus() != Match.MatchStatus.COMPLETED) {
            User user = userRepository.findByUsername(requestingUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return goalScorerPredictionRepository.findByUserAndMatch(user, match).stream()
                    .map(this::toGoalScorerResponse)
                    .toList();
        }

        return goalScorerPredictionRepository.findByMatch(match).stream()
                .map(this::toGoalScorerResponse)
                .toList();
    }

    public List<PredictionResponse> getMyPredictions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return predictionRepository.findByUser(user).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PredictionResponse> getPredictionsForMatch(Long matchId, String requestingUsername) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Only show other users' predictions after match is completed
        if (match.getStatus() != Match.MatchStatus.COMPLETED) {
            User user = userRepository.findByUsername(requestingUsername)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return predictionRepository.findByUserAndMatch(user, match)
                    .map(p -> List.of(toResponse(p)))
                    .orElse(List.of());
        }

        // Match is completed - show all predictions
        return predictionRepository.findByMatch(match).stream()
                .map(this::toResponse)
                .toList();
    }

    // Tournament predictions lock when tournament starts (June 11, 2026)
    private static final LocalDateTime TOURNAMENT_START = LocalDateTime.of(2026, 6, 11, 0, 0);

    private void checkTournamentPredictionLock() {
        if (LocalDateTime.now().isAfter(TOURNAMENT_START)) {
            throw new RuntimeException("Tournament predictions are locked. The World Cup has started.");
        }
    }

    public java.util.Map<String, Object> getMyTournamentPredictions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.Map<String, Object> result = new java.util.HashMap<>();

        topScorerPredictionRepository.findByUser(user).ifPresent(p ->
            result.put("topScorer", java.util.Map.of("playerName", p.getPlayerName(), "teamName", p.getTeamName() != null ? p.getTeamName() : ""))
        );

        goldenBallPredictionRepository.findByUser(user).ifPresent(p ->
            result.put("goldenBall", java.util.Map.of("playerName", p.getPlayerName(), "teamName", p.getTeamName() != null ? p.getTeamName() : ""))
        );

        goldenGlovePredictionRepository.findByUser(user).ifPresent(p ->
            result.put("goldenGlove", java.util.Map.of("playerName", p.getPlayerName(), "teamName", p.getTeamName() != null ? p.getTeamName() : ""))
        );

        worldCupWinnerPredictionRepository.findByUser(user).ifPresent(p ->
            result.put("worldCupWinner", java.util.Map.of("teamName", p.getTeam().getName(), "teamId", p.getTeam().getId()))
        );

        result.put("locked", LocalDateTime.now().isAfter(TOURNAMENT_START));

        return result;
    }

    public void predictTopScorer(String username, SpecialPredictionRequest request) {
        checkTournamentPredictionLock();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TopScorerPrediction prediction = topScorerPredictionRepository.findByUser(user)
                .orElse(TopScorerPrediction.builder().user(user).build());

        prediction.setPlayerName(request.getPlayerName());
        prediction.setTeamName(request.getTeamName());
        topScorerPredictionRepository.save(prediction);
    }

    public void predictGoldenBall(String username, SpecialPredictionRequest request) {
        checkTournamentPredictionLock();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GoldenBallPrediction prediction = goldenBallPredictionRepository.findByUser(user)
                .orElse(GoldenBallPrediction.builder().user(user).build());

        prediction.setPlayerName(request.getPlayerName());
        prediction.setTeamName(request.getTeamName());
        goldenBallPredictionRepository.save(prediction);
    }

    private PredictionResponse toResponse(Prediction prediction) {
        return PredictionResponse.builder()
                .id(prediction.getId())
                .matchId(prediction.getMatch().getId())
                .team1Name(prediction.getMatch().getTeam1().getName())
                .team2Name(prediction.getMatch().getTeam2().getName())
                .team1Flag(prediction.getMatch().getTeam1().getFlagUrl())
                .team2Flag(prediction.getMatch().getTeam2().getFlagUrl())
                .predictedTeam1Score(prediction.getPredictedTeam1Score())
                .predictedTeam2Score(prediction.getPredictedTeam2Score())
                .actualTeam1Score(prediction.getMatch().getTeam1Score())
                .actualTeam2Score(prediction.getMatch().getTeam2Score())
                .pointsEarned(prediction.getPointsEarned())
                .matchStatus(prediction.getMatch().getStatus().name())
                .username(prediction.getUser().getUsername())
                .build();
    }

    private GoalScorerPredictionResponse toGoalScorerResponse(GoalScorerPrediction gsp) {
        return GoalScorerPredictionResponse.builder()
                .id(gsp.getId())
                .matchId(gsp.getMatch().getId())
                .team1Name(gsp.getMatch().getTeam1().getName())
                .team2Name(gsp.getMatch().getTeam2().getName())
                .playerId(gsp.getPlayer().getId())
                .playerName(gsp.getPlayer().getName())
                .playerTeam(gsp.getPlayer().getTeam().getName())
                .firstGoalScorer(gsp.isFirstGoalScorer())
                .pointsEarned(gsp.getPointsEarned())
                .matchStatus(gsp.getMatch().getStatus().name())
                .username(gsp.getUser().getUsername())
                .build();
    }

    // ─── MOTM Prediction ──────────────────────────────────

    public void predictMotm(String username, Long matchId, Long playerId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        if (match.getMatchDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Predictions are locked. Match has already started.");
        }

        MotmPrediction prediction = motmPredictionRepository.findByUserAndMatch(user, match)
                .orElse(MotmPrediction.builder().user(user).match(match).build());
        prediction.setPlayer(player);
        motmPredictionRepository.save(prediction);
    }

    // ─── World Cup Winner Prediction ──────────────────────

    private static final LocalDateTime GROUP_STAGE_END = LocalDateTime.of(2026, 6, 27, 23, 59);

    public void predictWorldCupWinner(String username, Long teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Check if group stage is over
        if (LocalDateTime.now().isAfter(GROUP_STAGE_END)) {
            throw new RuntimeException("World Cup Winner prediction is locked after group stage ends (June 27).");
        }

        WorldCupWinnerPrediction prediction = worldCupWinnerPredictionRepository.findByUser(user)
                .orElse(WorldCupWinnerPrediction.builder().user(user).build());
        prediction.setTeam(team);
        worldCupWinnerPredictionRepository.save(prediction);
    }

    // ─── Golden Glove Prediction ──────────────────────────

    public void predictGoldenGlove(String username, SpecialPredictionRequest request) {
        checkTournamentPredictionLock();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GoldenGlovePrediction prediction = goldenGlovePredictionRepository.findByUser(user)
                .orElse(GoldenGlovePrediction.builder().user(user).build());

        prediction.setPlayerName(request.getPlayerName());
        prediction.setTeamName(request.getTeamName());
        goldenGlovePredictionRepository.save(prediction);
    }
}
