package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.GoldenBallPrediction;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoldenBallPredictionRepository extends JpaRepository<GoldenBallPrediction, Long> {
    Optional<GoldenBallPrediction> findByUser(User user);
    boolean existsByUser(User user);
}
