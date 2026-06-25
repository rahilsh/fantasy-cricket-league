package com.rsh.fcl.repository;

import com.rsh.fcl.model.Outcome;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutcomeRepository extends JpaRepository<Outcome, Long> {

  List<Outcome> findByGameIdOrderByIdAsc(Long gameId);
}
