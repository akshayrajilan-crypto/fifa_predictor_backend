package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchResultRequest {
    @NotNull
    private Long matchId;

    @Min(0)
    private int team1Score;

    @Min(0)
    private int team2Score;
}
