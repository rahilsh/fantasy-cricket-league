package com.rsh.fcl.repository;

import com.rsh.fcl.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, String> {
}
