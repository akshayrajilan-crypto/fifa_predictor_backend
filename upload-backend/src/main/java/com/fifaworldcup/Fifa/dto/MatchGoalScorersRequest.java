package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MatchGoalScorersRequest {
    @NotNull
    private Long matchId;

    @NotNull
    private List<Long> scorerPlayerIds;  // actual goal scorers

    private Long firstGoalScorerPlayerId;  // actual first goal scorer
}
