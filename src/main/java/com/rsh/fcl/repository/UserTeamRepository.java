package com.rsh.fcl.repository;

import com.rsh.fcl.model.UserTeam;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTeamRepository extends JpaRepository<UserTeam, Long> {

  List<UserTeam> findByGameId(Long gameId);

  Optional<UserTeam> findByGameIdAndUser_UserName(Long gameId, String userName);

  boolean existsByGameIdAndUser_UserName(Long gameId, String userName);
}
