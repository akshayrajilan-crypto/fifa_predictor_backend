package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String label;  // e.g., "For Rahul", "For Priya" — admin's note

    @Builder.Default
    private boolean used = false;

    private String usedByUsername;  // who used it

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime usedAt;
}
