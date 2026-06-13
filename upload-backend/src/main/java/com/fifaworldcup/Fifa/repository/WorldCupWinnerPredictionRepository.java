package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.User;
import com.fifaworldcup.Fifa.model.WorldCupWinnerPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorldCupWinnerPredictionRepository extends JpaRepository<WorldCupWinnerPrediction, Long> {
    Optional<WorldCupWinnerPrediction> findByUser(User user);
    List<WorldCupWinnerPrediction> findByScored(boolean scored);
}
