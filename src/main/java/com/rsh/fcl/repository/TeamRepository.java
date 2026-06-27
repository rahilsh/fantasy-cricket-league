package com.rsh.fcl.repository;

import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

  @Query("select team from Team team join team.cricketers cricketer "
      + "where cricketer.globalUniqueId = :cricketerId "
      + "and team.tournament.status <> :excludedStatus")
  List<Team> findActiveTeamsForCricketer(
      @Param("cricketerId") String cricketerId,
      @Param("excludedStatus") TournamentStatus excludedStatus);
}
