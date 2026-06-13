package com.fifaworldcup.Fifa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private String username;
    private int totalPoints;
    private int correctScores;
    private int correctResults;
}
