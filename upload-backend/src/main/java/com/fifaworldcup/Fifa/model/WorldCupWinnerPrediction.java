package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "world_cup_winner_predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorldCupWinnerPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Builder.Default
    private int pointsEarned = 0;

    @Builder.Default
    private boolean scored = false;

    @Builder.Default
    private boolean locked = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
