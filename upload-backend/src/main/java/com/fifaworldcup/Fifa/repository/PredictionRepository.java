package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.Prediction;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    Optional<Prediction> findByUserAndMatch(User user, Match match);
    List<Prediction> findByUser(User user);
    List<Prediction> findByMatch(Match match);
    List<Prediction> findByMatchAndScored(Match match, boolean scored);
    boolean existsByUserAndMatch(User user, Match match);
}
