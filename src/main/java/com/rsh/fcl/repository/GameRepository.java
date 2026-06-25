package com.rsh.fcl.repository;

import com.rsh.fcl.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
