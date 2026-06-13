package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_goal_scorers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchGoalScorer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    private String playerName;

    @Builder.Default
    @Column(name = "goal_minute")
    private int minute = 0;

    @Builder.Default
    private boolean firstGoal = false;

    @Builder.Default
    private boolean ownGoal = false;

    @Builder.Default
    private boolean penalty = false;
}
