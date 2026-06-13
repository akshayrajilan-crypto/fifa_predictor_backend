package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.GoldenGlovePrediction;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoldenGlovePredictionRepository extends JpaRepository<GoldenGlovePrediction, Long> {
    Optional<GoldenGlovePrediction> findByUser(User user);
}
