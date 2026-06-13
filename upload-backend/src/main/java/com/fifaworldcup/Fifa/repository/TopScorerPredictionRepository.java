package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.TopScorerPrediction;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopScorerPredictionRepository extends JpaRepository<TopScorerPrediction, Long> {
    Optional<TopScorerPrediction> findByUser(User user);
    boolean existsByUser(User user);
}
