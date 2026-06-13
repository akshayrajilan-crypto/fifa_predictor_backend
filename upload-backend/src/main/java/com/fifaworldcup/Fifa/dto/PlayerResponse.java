package com.fifaworldcup.Fifa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerResponse {
    private Long id;
    private String name;
    private String teamName;
    private Long teamId;
    private String position;
}
