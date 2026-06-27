package com.rsh.fcl.repository;

import com.rsh.fcl.model.Player;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

  List<Player> findByGlobalUniqueIdIn(Collection<Long> globalUniqueIds);
}
