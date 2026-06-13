package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "golden_glove_predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenGlovePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String playerName;

    private String teamName;

    @Builder.Default
    private int pointsEarned = 0;

    @Builder.Default
    private boolean scored = false;
}
