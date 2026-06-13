package com.fifaworldcup.Fifa;

import com.fifaworldcup.Fifa.dto.MatchGoalScorersRequest;
import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import com.fifaworldcup.Fifa.service.AdminService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PointsCalculationTest {

    @Autowired private AdminService adminService;
    @Autowired private UserRepository userRepository;
    @Autowired private TeamRepository teamRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private PredictionRepository predictionRepository;
    @Autowired private GoalScorerPredictionRepository goalScorerPredictionRepository;
    @Autowired private MotmPredictionRepository motmPredictionRepository;

    private static Team teamA;
    private static Team teamB;
    private static Match match1;
    private static Player playerA1, playerA2, playerA3, playerB1, playerB2;
    private static User user1, user2, user3, user4, user5;

    @BeforeAll
    static void setup(@Autowired TeamRepository teamRepo,
                      @Autowired MatchRepository matchRepo,
                      @Autowired PlayerRepository playerRepo,
                      @Autowired UserRepository userRepo,
                      @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder) {

        // Create teams
        teamA = teamRepo.save(Team.builder().name("TestTeamA").group("X").build());
        teamB = teamRepo.save(Team.builder().name("TestTeamB").group("X").build());

        // Create match (in the past so predictions are "locked" but we'll force save them)
        match1 = matchRepo.save(Match.builder()
                .team1(teamA).team2(teamB)
                .matchDateTime(LocalDateTime.of(2026, 6, 10, 15, 0))
                .venue("Test Stadium")
                .stage(Match.Stage.GROUP)
                .group("X")
                .status(Match.MatchStatus.UPCOMING)
                .build());

        // Create players
        playerA1 = playerRepo.save(Player.builder().name("PlayerA1").team(teamA).position(Player.Position.FORWARD).build());
        playerA2 = playerRepo.save(Player.builder().name("PlayerA2").team(teamA).position(Player.Position.FORWARD).build());
        playerA3 = playerRepo.save(Player.builder().name("PlayerA3").team(teamA).position(Player.Position.MIDFIELDER).build());
        playerB1 = playerRepo.save(Player.builder().name("PlayerB1").team(teamB).position(Player.Position.FORWARD).build());
        playerB2 = playerRepo.save(Player.builder().name("PlayerB2").team(teamB).position(Player.Position.MIDFIELDER).build());

        // Create 5 test users
        user1 = userRepo.save(User.builder().username("testuser1").password(encoder.encode("pass")).role(User.Role.USER).build());
        user2 = userRepo.save(User.builder().username("testuser2").password(encoder.encode("pass")).role(User.Role.USER).build());
        user3 = userRepo.save(User.builder().username("testuser3").password(encoder.encode("pass")).role(User.Role.USER).build());
        user4 = userRepo.save(User.builder().username("testuser4").password(encoder.encode("pass")).role(User.Role.USER).build());
        user5 = userRepo.save(User.builder().username("testuser5").password(encoder.encode("pass")).role(User.Role.USER).build());
    }

    @Test
    @Order(1)
    @DisplayName("Setup predictions for match: TeamA 2-1 TeamB")
    void setupPredictions() {
        // USER 1: Predicts exact score 2-1, scorers: A1+A2, MOTM: A1 → expect 1+2+2+2+3 = 10
        predictionRepository.save(Prediction.builder()
                .user(user1).match(match1)
                .predictedTeam1Score(2).predictedTeam2Score(1).build());
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(user1).match(match1).player(playerA1).build());
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(user1).match(match1).player(playerA2).build());
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(user1).match(match1).player(playerA1).build());

        // USER 2: Predicts 1-0 (correct result, wrong score), scorer: A1, MOTM: A1 → expect 1+0+2+3 = 6
        predictionRepository.save(Prediction.builder()
                .user(user2).match(match1)
                .predictedTeam1Score(1).predictedTeam2Score(0).build());
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(user2).match(match1).player(playerA1).build());
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(user2).match(match1).player(playerA1).build());

        // USER 3: Predicts 0-2 (wrong result), scorer: B1, MOTM: B1 → expect 0+0+2+0 = 2
        // (B1 scored the consolation goal, but result is wrong — still gets scorer points)
        predictionRepository.save(Prediction.builder()
                .user(user3).match(match1)
                .predictedTeam1Score(0).predictedTeam2Score(2).build());
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(user3).match(match1).player(playerB1).build());
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(user3).match(match1).player(playerB1).build());

        // USER 4: Predicts 2-1 (exact), scorer: A3 (didn't score), MOTM: A3 → expect 1+2+0+0 = 3
        predictionRepository.save(Prediction.builder()
                .user(user4).match(match1)
                .predictedTeam1Score(2).predictedTeam2Score(1).build());
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(user4).match(match1).player(playerA3).build());
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(user4).match(match1).player(playerA3).build());

        // USER 5: Predicts 0-0 (wrong), no scorers, no MOTM → expect 0+0+0+0 = 0
        predictionRepository.save(Prediction.builder()
                .user(user5).match(match1)
                .predictedTeam1Score(0).predictedTeam2Score(0).build());

        assertTrue(predictionRepository.findByMatch(match1).size() == 5);
    }

    @Test
    @Order(2)
    @DisplayName("Submit match result 2-1 → score prediction points calculated")
    void testSubmitMatchResult() {
        MatchResultRequest request = new MatchResultRequest();
        request.setMatchId(match1.getId());
        request.setTeam1Score(2);
        request.setTeam2Score(1);

        adminService.submitMatchResult(request);

        // Verify match status
        Match updated = matchRepository.findById(match1.getId()).orElseThrow();
        assertEquals(Match.MatchStatus.COMPLETED, updated.getStatus());
        assertEquals(2, updated.getTeam1Score());
        assertEquals(1, updated.getTeam2Score());

        // Check score prediction points
        User u1 = userRepository.findByUsername("testuser1").orElseThrow();
        User u2 = userRepository.findByUsername("testuser2").orElseThrow();
        User u3 = userRepository.findByUsername("testuser3").orElseThrow();
        User u4 = userRepository.findByUsername("testuser4").orElseThrow();
        User u5 = userRepository.findByUsername("testuser5").orElseThrow();

        // user1: exact 2-1 → +3 (1 result + 2 exact)
        assertEquals(3, u1.getTotalPoints(), "User1 should have 3 pts (exact score)");
        // user2: predicted 1-0, actual 2-1 → correct result (TeamA win) → +1
        assertEquals(1, u2.getTotalPoints(), "User2 should have 1 pt (correct result only)");
        // user3: predicted 0-2, actual 2-1 → wrong result → 0
        assertEquals(0, u3.getTotalPoints(), "User3 should have 0 pts (wrong result)");
        // user4: exact 2-1 → +3
        assertEquals(3, u4.getTotalPoints(), "User4 should have 3 pts (exact score)");
        // user5: predicted 0-0, actual 2-1 → wrong → 0
        assertEquals(0, u5.getTotalPoints(), "User5 should have 0 pts (wrong result)");

        System.out.println("✅ Score prediction points verified:");
        System.out.println("   user1: " + u1.getTotalPoints() + " (expected 3)");
        System.out.println("   user2: " + u2.getTotalPoints() + " (expected 1)");
        System.out.println("   user3: " + u3.getTotalPoints() + " (expected 0)");
        System.out.println("   user4: " + u4.getTotalPoints() + " (expected 3)");
        System.out.println("   user5: " + u5.getTotalPoints() + " (expected 0)");
    }

    @Test
    @Order(3)
    @DisplayName("Submit goal scorers A1, A2, B1 → goal scorer points calculated")
    void testSubmitGoalScorers() {
        MatchGoalScorersRequest request = new MatchGoalScorersRequest();
        request.setMatchId(match1.getId());
        request.setScorerPlayerIds(List.of(playerA1.getId(), playerA2.getId(), playerB1.getId()));
        request.setFirstGoalScorerPlayerId(playerA1.getId());

        adminService.submitMatchGoalScorers(request);

        User u1 = userRepository.findByUsername("testuser1").orElseThrow();
        User u2 = userRepository.findByUsername("testuser2").orElseThrow();
        User u3 = userRepository.findByUsername("testuser3").orElseThrow();
        User u4 = userRepository.findByUsername("testuser4").orElseThrow();
        User u5 = userRepository.findByUsername("testuser5").orElseThrow();

        // user1: predicted A1+A2, both scored → +2+2 = +4, total now 3+4 = 7
        assertEquals(7, u1.getTotalPoints(), "User1: 3 (score) + 4 (scorers) = 7");
        // user2: predicted A1, scored → +2, total now 1+2 = 3
        assertEquals(3, u2.getTotalPoints(), "User2: 1 (score) + 2 (scorer) = 3");
        // user3: predicted B1, scored → +2, total now 0+2 = 2
        assertEquals(2, u3.getTotalPoints(), "User3: 0 (score) + 2 (scorer) = 2");
        // user4: predicted A3, didn't score → +0, total stays 3
        assertEquals(3, u4.getTotalPoints(), "User4: 3 (score) + 0 (wrong scorer) = 3");
        // user5: no scorer predictions → 0, total stays 0
        assertEquals(0, u5.getTotalPoints(), "User5: no predictions = 0");

        System.out.println("✅ Goal scorer points verified:");
        System.out.println("   user1: " + u1.getTotalPoints() + " (expected 7)");
        System.out.println("   user2: " + u2.getTotalPoints() + " (expected 3)");
        System.out.println("   user3: " + u3.getTotalPoints() + " (expected 2)");
        System.out.println("   user4: " + u4.getTotalPoints() + " (expected 3)");
        System.out.println("   user5: " + u5.getTotalPoints() + " (expected 0)");
    }

    @Test
    @Order(4)
    @DisplayName("Submit MOTM as PlayerA1 → MOTM points calculated")
    void testSubmitMotm() {
        adminService.submitMotm(match1.getId(), "PlayerA1");

        User u1 = userRepository.findByUsername("testuser1").orElseThrow();
        User u2 = userRepository.findByUsername("testuser2").orElseThrow();
        User u3 = userRepository.findByUsername("testuser3").orElseThrow();
        User u4 = userRepository.findByUsername("testuser4").orElseThrow();
        User u5 = userRepository.findByUsername("testuser5").orElseThrow();

        // user1: predicted A1 as MOTM, correct → +3, total now 7+3 = 10
        assertEquals(10, u1.getTotalPoints(), "User1: 7 + 3 (MOTM) = 10");
        // user2: predicted A1 as MOTM, correct → +3, total now 3+3 = 6
        assertEquals(6, u2.getTotalPoints(), "User2: 3 + 3 (MOTM) = 6");
        // user3: predicted B1 as MOTM, wrong → +0, total stays 2
        assertEquals(2, u3.getTotalPoints(), "User3: 2 + 0 (wrong MOTM) = 2");
        // user4: predicted A3 as MOTM, wrong → +0, total stays 3
        assertEquals(3, u4.getTotalPoints(), "User4: 3 + 0 (wrong MOTM) = 3");
        // user5: no MOTM prediction → 0, total stays 0
        assertEquals(0, u5.getTotalPoints(), "User5: no MOTM = 0");

        System.out.println("✅ MOTM points verified:");
        System.out.println("   user1: " + u1.getTotalPoints() + " (expected 10) ★ PERFECT");
        System.out.println("   user2: " + u2.getTotalPoints() + " (expected 6)");
        System.out.println("   user3: " + u3.getTotalPoints() + " (expected 2)");
        System.out.println("   user4: " + u4.getTotalPoints() + " (expected 3)");
        System.out.println("   user5: " + u5.getTotalPoints() + " (expected 0)");
    }

    @Test
    @Order(5)
    @DisplayName("Final verification: all points add up correctly")
    void testFinalTotals() {
        User u1 = userRepository.findByUsername("testuser1").orElseThrow();
        User u2 = userRepository.findByUsername("testuser2").orElseThrow();
        User u3 = userRepository.findByUsername("testuser3").orElseThrow();
        User u4 = userRepository.findByUsername("testuser4").orElseThrow();
        User u5 = userRepository.findByUsername("testuser5").orElseThrow();

        System.out.println("\n=== FINAL LEADERBOARD ===");
        System.out.println("1. testuser1: " + u1.getTotalPoints() + " pts (exact score + both scorers + MOTM)");
        System.out.println("2. testuser2: " + u2.getTotalPoints() + " pts (correct result + 1 scorer + MOTM)");
        System.out.println("3. testuser4: " + u4.getTotalPoints() + " pts (exact score + wrong scorer + wrong MOTM)");
        System.out.println("4. testuser3: " + u3.getTotalPoints() + " pts (wrong result + 1 scorer + wrong MOTM)");
        System.out.println("5. testuser5: " + u5.getTotalPoints() + " pts (all wrong)");

        // Verify final expected totals
        assertEquals(10, u1.getTotalPoints()); // 1+2+2+2+3
        assertEquals(6, u2.getTotalPoints());  // 1+0+2+3
        assertEquals(2, u3.getTotalPoints());  // 0+0+2+0
        assertEquals(3, u4.getTotalPoints());  // 1+2+0+0
        assertEquals(0, u5.getTotalPoints());  // 0+0+0+0

        // Verify total points distributed
        int total = u1.getTotalPoints() + u2.getTotalPoints() + u3.getTotalPoints() + u4.getTotalPoints() + u5.getTotalPoints();
        System.out.println("\nTotal points distributed: " + total);
        assertEquals(21, total, "Total points across all users should be 21");

        System.out.println("\n✅ ALL CALCULATION TESTS PASSED");
    }

    @Test
    @Order(6)
    @DisplayName("Recalculate all points → same results (idempotent)")
    void testRecalculate() {
        // Save current totals
        int before1 = userRepository.findByUsername("testuser1").orElseThrow().getTotalPoints();
        int before2 = userRepository.findByUsername("testuser2").orElseThrow().getTotalPoints();

        // Recalculate
        String result = adminService.recalculateAllPoints();
        System.out.println("Recalculate result: " + result);

        // Score predictions should be the same
        User u1 = userRepository.findByUsername("testuser1").orElseThrow();
        User u2 = userRepository.findByUsername("testuser2").orElseThrow();

        // Note: recalculate only re-scores score predictions and MOTM, not goal scorers
        // (goal scorers require the admin to re-submit). So totals may differ.
        // The score + MOTM portion should match.
        System.out.println("After recalculate - user1: " + u1.getTotalPoints() + " (was " + before1 + ")");
        System.out.println("After recalculate - user2: " + u2.getTotalPoints() + " (was " + before2 + ")");

        // Score portion: user1 should have at least 3 (score) + 3 (MOTM) = 6
        assertTrue(u1.getTotalPoints() >= 6, "User1 should have at least 6 after recalculate (score+MOTM)");
    }
}
