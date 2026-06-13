package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.LeaderboardEntry;
import com.fifaworldcup.Fifa.model.Prediction;
import com.fifaworldcup.Fifa.model.User;
import com.fifaworldcup.Fifa.repository.PredictionRepository;
import com.fifaworldcup.Fifa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;

    public List<LeaderboardEntry> getLeaderboard() {
        List<User> users = userRepository.findAllByOrderByTotalPointsDesc();
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        int rank = 1;
        for (User user : users) {
            // Exclude admin from leaderboard
            if (user.getRole() == User.Role.ADMIN) continue;

            List<Prediction> predictions = predictionRepository.findByUser(user);
            int correctScores = (int) predictions.stream()
                    .filter(p -> p.getPointsEarned() == 3)  // exact score = 1 (result) + 2 (exact) = 3
                    .count();
            int correctResults = (int) predictions.stream()
                    .filter(p -> p.getPointsEarned() == 1)  // correct result only = 1
                    .count();

            leaderboard.add(new LeaderboardEntry(
                    rank++,
                    user.getUsername(),
                    user.getTotalPoints(),
                    correctScores,
                    correctResults
            ));
        }
        return leaderboard;
    }
}
