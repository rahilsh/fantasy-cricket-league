package com.rsh.fcl.service;

import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Outcome;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.OutcomeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutcomeService {

  private final OutcomeRepository outcomeRepository;
  private final GameRepository gameRepository;

  public OutcomeService(OutcomeRepository outcomeRepository, GameRepository gameRepository) {
    this.outcomeRepository = outcomeRepository;
    this.gameRepository = gameRepository;
  }

  @Transactional
  public Outcome createOutcome(long gameId, int batsman, int bowler, int score) {
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    return outcomeRepository.save(new Outcome(game, batsman, bowler, score));
  }

  @Transactional(readOnly = true)
  public List<Outcome> getOutcomes() {
    return outcomeRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<Outcome> getOutcomesForGame(long gameId) {
    if (!gameRepository.existsById(gameId)) {
      throw new GameNotFoundException(gameId);
    }
    return outcomeRepository.findByGameIdOrderByIdAsc(gameId);
  }

  @Transactional(readOnly = true)
  public Outcome getOutcome(long id) {
    return outcomeRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Outcome", id));
  }

  @Transactional
  public Outcome updateOutcome(long id, long gameId, int batsman, int bowler, int score) {
    Outcome outcome = getOutcome(id);
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    outcome.setGame(game);
    outcome.setBatsman(batsman);
    outcome.setBowler(bowler);
    outcome.setScore(score);
    return outcomeRepository.save(outcome);
  }

  @Transactional
  public void deleteOutcome(long id) {
    outcomeRepository.delete(getOutcome(id));
  }
}
