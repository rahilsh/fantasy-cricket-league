package com.rsh.fcl.repository;

import com.rsh.fcl.model.BallEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {

  List<BallEvent> findByGameIdOrderByIdAsc(Long gameId);
}
