package com.fifaworldcup.Fifa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InviteCodeResponse {
    private Long id;
    private String code;
    private String label;
    private boolean used;
    private String usedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
