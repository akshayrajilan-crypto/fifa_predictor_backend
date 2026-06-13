package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GoalScorerPredictionRequest {
    @NotNull
    private Long matchId;

    @NotNull
    private List<Long> playerIds;  // players predicted to score

    private Long firstGoalScorerPlayerId;  // optional: predict first goal scorer for extra bonus
}
