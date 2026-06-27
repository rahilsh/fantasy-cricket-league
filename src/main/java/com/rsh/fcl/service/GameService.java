package com.rsh.fcl.service;

import static com.rsh.fcl.model.Game.GameStatus;

import com.rsh.fcl.exception.BallEventNotSupportedException;
import com.rsh.fcl.exception.GameAlreadyCompletedException;
import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.GameNotStartedException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.repository.TournamentRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

  private static final Comparator<UserTeam> BEST_TOP_USER_FIRST = Comparator
      .comparingDouble(UserTeam::getPoints)
      .reversed()
      .thenComparing(UserTeam::getUserName);

  private final GameRepository gameRepository;
  private final TournamentRepository tournamentRepository;
  private final TeamRepository teamRepository;
  private final UserTeamRepository userTeamRepository;
  private final BallEventRepository ballEventRepository;

  public GameService(
      GameRepository gameRepository,
      TournamentRepository tournamentRepository,
      TeamRepository teamRepository,
      UserTeamRepository userTeamRepository,
      BallEventRepository ballEventRepository) {
    this.gameRepository = gameRepository;
    this.tournamentRepository = tournamentRepository;
    this.teamRepository = teamRepository;
    this.userTeamRepository = userTeamRepository;
    this.ballEventRepository = ballEventRepository;
  }

  @Transactional
  public Game createGame(long tournamentId, long team1Id, long team2Id, int k, int overs) {
    validateTopK(k);
    validateOvers(overs);
    Tournament tournament = findTournament(tournamentId);
    if (tournament.getStatus() == TournamentStatus.COMPLETED) {
      throw new IllegalArgumentException("Cannot create games in a completed tournament");
    }
    Team team1 = resolveTeam(tournament, team1Id);
    Team team2 = resolveTeam(tournament, team2Id);
    if (team1.getId().equals(team2.getId())) {
      throw new IllegalArgumentException("A game must be between two distinct teams");
    }
    SquadRules.validateComposition(team1.getCricketers(), "Team " + team1.getName());
    SquadRules.validateComposition(team2.getCricketers(), "Team " + team2.getName());
    return gameRepository.save(new Game(tournament, team1, team2, k, overs));
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
  public Game updateGame(long gameId, long tournamentId, long team1Id, long team2Id, int k,
      int overs) {
    validateTopK(k);
    validateOvers(overs);
    Game game = findGame(gameId);
    Tournament tournament = findTournament(tournamentId);
    Team team1 = resolveTeam(tournament, team1Id);
    Team team2 = resolveTeam(tournament, team2Id);
    if (team1.getId().equals(team2.getId())) {
      throw new IllegalArgumentException("A game must be between two distinct teams");
    }
    SquadRules.validateComposition(team1.getCricketers(), "Team " + team1.getName());
    SquadRules.validateComposition(team2.getCricketers(), "Team " + team2.getName());
    game.setTournament(tournament);
    game.setTeam1(team1);
    game.setTeam2(team2);
    game.setK(k);
    game.setOvers(overs);
    return gameRepository.save(game);
  }

  @Transactional
  public void deleteGame(long gameId) {
    gameRepository.delete(findGame(gameId));
  }

  @Transactional
  public Game startGame(long gameId) {
    Game game = findGame(gameId);
    if (game.getStatus().equals(GameStatus.COMPLETED)) {
      throw new GameAlreadyCompletedException(gameId);
    }
    ensureTeamsAreFree(game);
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
  public BallEvent play(long gameId, String batsman, String bowler, int outcomeScore) {
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

  private void ensureTeamsAreFree(Game game) {
    for (Team team : List.of(game.getTeam1(), game.getTeam2())) {
      boolean busy = gameRepository.findByStatusAndTeam(GameStatus.IN_PROGRESS, team.getId())
          .stream()
          .anyMatch(other -> !other.getId().equals(game.getId()));
      if (busy) {
        throw new IllegalArgumentException(
            "Team " + team.getName() + " is already in another in-progress game");
      }
    }
  }

  private Team resolveTeam(Tournament tournament, long teamId) {
    Team team = teamRepository.findById(teamId)
        .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    if (!team.getTournament().getId().equals(tournament.getId())) {
      throw new IllegalArgumentException(
          "Team " + teamId + " does not belong to tournament " + tournament.getId());
    }
    return team;
  }

  private Tournament findTournament(long tournamentId) {
    return tournamentRepository.findById(tournamentId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
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
      String batsman,
      String bowler,
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

  private static void updatePoints(List<UserTeam> userTeams, String cricketerId, double delta) {
    userTeams.stream()
        .filter(userTeam -> userTeam.hasPlayer(cricketerId))
        .forEach(userTeam -> userTeam.setPoints(userTeam.getPoints() + delta));
  }
}
