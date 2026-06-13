package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.MatchGoalScorer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchGoalScorerRepository extends JpaRepository<MatchGoalScorer, Long> {
    List<MatchGoalScorer> findByMatchOrderByMinuteAsc(Match match);
    List<MatchGoalScorer> findByMatchId(Long matchId);
    void deleteByMatch(Match match);
}
