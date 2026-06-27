package com.rsh.fcl.service;

import static com.rsh.fcl.model.Game.GameStatus;

import com.rsh.fcl.exception.GameAlreadyCompletedException;
import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.GameNotStartedException;
import com.rsh.fcl.exception.BallEventNotSupportedException;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

  private static final Comparator<UserTeam> BEST_TOP_USER_FIRST = Comparator
      .comparingDouble(UserTeam::getPoints)
      .reversed()
      .thenComparing(UserTeam::getUserName);

  private final GameRepository gameRepository;
  private final UserTeamRepository userTeamRepository;
  private final BallEventRepository ballEventRepository;

  public GameService(
      GameRepository gameRepository,
      UserTeamRepository userTeamRepository,
      BallEventRepository ballEventRepository) {
    this.gameRepository = gameRepository;
    this.userTeamRepository = userTeamRepository;
    this.ballEventRepository = ballEventRepository;
  }

  @Transactional
  public Game createGame(String team1, String team2, int k, int overs) {
    validateTopK(k);
    validateOvers(overs);
    return gameRepository.save(new Game(team1, team2, k, overs));
  }

  @Transactional(readOnly = true)
  public Page<Game> getGames(Pageable pageable) {
    return gameRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Game getGame(long gameId) {
    return findGame(gameId);
  }

  @Transactional
  public Game updateGame(long gameId, String team1, String team2, int k, int overs) {
    validateTopK(k);
    validateOvers(overs);
    Game game = findGame(gameId);
    game.setTeam1(team1);
    game.setTeam2(team2);
    game.setK(k);
    game.setOvers(overs);
    return gameRepository.save(game);
  }

  @Transactional
  public void deleteGame(long gameId) {
    Game game = findGame(gameId);
    gameRepository.delete(game);
  }

  @Transactional
  public Game startGame(long gameId) {
    Game game = findGame(gameId);
    if (game.getStatus().equals(GameStatus.COMPLETED)) {
      throw new GameAlreadyCompletedException(gameId);
    }
    game.setStatus(GameStatus.IN_PROGRESS);
    return gameRepository.save(game);
  }

  @Transactional
  public Game endGame(long gameId) {
    Game game = findGame(gameId);
    if (game.getStatus().equals(GameStatus.CREATED)) {
      throw new GameNotStartedException(gameId);
    }
    game.setStatus(GameStatus.COMPLETED);
    return gameRepository.save(game);
  }

  @Transactional
  public BallEvent play(long gameId, int batsman, int bowler, int outcomeScore) {
    Game game = findGame(gameId);
    validateGameState(gameId, game);

    List<UserTeam> userTeamsForGame = userTeamRepository.findByGameIdForUpdate(gameId);
    applyBallEvent(outcomeScore, batsman, bowler, userTeamsForGame);
    userTeamRepository.saveAll(userTeamsForGame);

    game.setBallsBowled(game.getBallsBowled() + 1);
    if (outcomeScore == -1) {
      game.setWickets(game.getWickets() + 1);
    }
    if (game.isInningsOver()) {
      game.setStatus(GameStatus.COMPLETED);
    }
    gameRepository.save(game);

    return ballEventRepository.save(new BallEvent(game, batsman, bowler, outcomeScore));
  }

  @Transactional(readOnly = true)
  public List<UserTeam> getLeaderboard(long gameId) {
    Game game = findGame(gameId);
    return userTeamRepository.findByGameId(gameId)
        .stream()
        .sorted(BEST_TOP_USER_FIRST)
        .limit(game.getK())
        .toList();
  }

  private Game findGame(long gameId) {
    return gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
  }

  private static void validateTopK(int k) {
    if (k <= 0) {
      throw new IllegalArgumentException("Top K value must be greater than zero");
    }
  }

  private static void validateOvers(int overs) {
    if (overs <= 0) {
      throw new IllegalArgumentException("Overs must be greater than zero");
    }
  }

  private static void validateGameState(long gameId, Game game) {
    if (game.getStatus().equals(GameStatus.CREATED)) {
      throw new GameNotStartedException(gameId);
    } else if (game.getStatus().equals(GameStatus.COMPLETED)) {
      throw new GameAlreadyCompletedException(gameId);
    }
  }

  private static void applyBallEvent(
      int outcome,
      int batsman,
      int bowler,
      List<UserTeam> userTeamsForGame) {
    switch (outcome) {
      case 1 -> updatePoints(userTeamsForGame, batsman, 0.5);
      case 2 -> {
        updatePoints(userTeamsForGame, batsman, 1.0);
        updatePoints(userTeamsForGame, bowler, -0.5);
      }
      case 4 -> {
        updatePoints(userTeamsForGame, batsman, 2.0);
        updatePoints(userTeamsForGame, bowler, -1.0);
      }
      case 6 -> {
        updatePoints(userTeamsForGame, batsman, 3.0);
        updatePoints(userTeamsForGame, bowler, -2.0);
      }
      case -1 -> {
        updatePoints(userTeamsForGame, batsman, -2.0);
        updatePoints(userTeamsForGame, bowler, 4.0);
      }
      default -> throw new BallEventNotSupportedException(outcome);
    }
  }

  private static void updatePoints(List<UserTeam> userTeams, int playerId, double delta) {
    userTeams.stream()
        .filter(userTeam -> userTeam.hasPlayer(playerId))
        .forEach(userTeam -> userTeam.setPoints(userTeam.getPoints() + delta));
  }
}
