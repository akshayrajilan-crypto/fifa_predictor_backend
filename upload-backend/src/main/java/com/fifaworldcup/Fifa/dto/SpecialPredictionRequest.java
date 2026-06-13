package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpecialPredictionRequest {
    @NotBlank
    private String playerName;

    private String teamName;
}
