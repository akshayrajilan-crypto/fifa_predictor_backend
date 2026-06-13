package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.PlayerResponse;
import com.fifaworldcup.Fifa.model.Player;
import com.fifaworldcup.Fifa.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> getAllPlayers() {
        return ResponseEntity.ok(playerRepository.findAll().stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/search")
    public ResponseEntity<List<PlayerResponse>> searchPlayers(@RequestParam String q) {
        return ResponseEntity.ok(playerRepository.searchByName(q).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<PlayerResponse>> getPlayersByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(playerRepository.findByTeamId(teamId).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/forwards")
    public ResponseEntity<List<PlayerResponse>> getForwards() {
        return ResponseEntity.ok(playerRepository.findByPosition(Player.Position.FORWARD).stream()
                .map(this::toResponse)
                .toList());
    }

    private PlayerResponse toResponse(Player player) {
        return PlayerResponse.builder()
                .id(player.getId())
                .name(player.getName())
                .teamName(player.getTeam().getName())
                .teamId(player.getTeam().getId())
                .position(player.getPosition().name())
                .build();
    }
}
