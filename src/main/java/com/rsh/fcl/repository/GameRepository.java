package com.rsh.fcl.repository;

import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

  @Query("select game from Game game "
      + "where game.status = :status "
      + "and (game.team1.id = :teamId or game.team2.id = :teamId)")
  List<Game> findByStatusAndTeam(
      @Param("status") GameStatus status,
      @Param("teamId") Long teamId);
}
