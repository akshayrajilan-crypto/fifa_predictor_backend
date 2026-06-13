package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Player;
import com.fifaworldcup.Fifa.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByTeam(Team team);
    List<Player> findByTeamId(Long teamId);
    List<Player> findByPosition(Player.Position position);

    @Query("SELECT p FROM Player p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Player> searchByName(@Param("query") String query);
}
