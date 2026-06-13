package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "top_scorer_predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopScorerPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String playerName;

    private String teamName;

    private int pointsEarned = 0;

    private boolean scored = false;
}
