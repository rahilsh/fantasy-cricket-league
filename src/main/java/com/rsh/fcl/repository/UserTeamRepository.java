package com.rsh.fcl.repository;

import com.rsh.fcl.model.UserTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

  List<UserTeam> findByGameId(Long gameId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select userTeam from UserTeam userTeam where userTeam.game.id = :gameId")
  List<UserTeam> findByGameIdForUpdate(@Param("gameId") Long gameId);

  Optional<UserTeam> findByGameIdAndUser_UserName(Long gameId, String userName);

  boolean existsByGameIdAndUser_UserName(Long gameId, String userName);
}
