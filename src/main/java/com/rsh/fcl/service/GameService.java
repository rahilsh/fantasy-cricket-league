package com.rsh.fcl.service;

import static com.rsh.fcl.model.Game.GameStatus;

import com.rsh.fcl.exception.GameAlreadyCompletedException;
import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.GameNotStartedException;
import com.rsh.fcl.exception.BallEventNotSupportedException;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.PlayerType;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.dto.PlayerRequest;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
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
  private final ReadablePlayerIdGenerator playerIdGenerator;

  public GameService(
      GameRepository gameRepository,
      UserTeamRepository userTeamRepository,
      BallEventRepository ballEventRepository,
      ReadablePlayerIdGenerator playerIdGenerator) {
    this.gameRepository = gameRepository;
    this.userTeamRepository = userTeamRepository;
    this.ballEventRepository = ballEventRepository;
    this.playerIdGenerator = playerIdGenerator;
  }

  @Transactional
  public Game createGame(
      String team1,
      String team2,
      int k,
      int overs,
      List<PlayerRequest> team1Players,
      List<PlayerRequest> team2Players) {
    validateTopK(k);
    validateOvers(overs);
    Game game = new Game(k, overs);
    applyTeams(game, team1, team1Players, team2, team2Players);
    validateRoster(game);
    return gameRepository.save(game);
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
  public Game updateGame(
      long gameId,
      String team1,
      String team2,
      int k,
      int overs,
      List<PlayerRequest> team1Players,
      List<PlayerRequest> team2Players) {
    validateTopK(k);
    validateOvers(overs);
    Game game = findGame(gameId);
    game.setK(k);
    game.setOvers(overs);
    applyTeams(game, team1, team1Players, team2, team2Players);
    validateRoster(game);
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

  private static void updatePoints(List<UserTeam> userTeams, String playerId, double delta) {
    userTeams.stream()
        .filter(userTeam -> userTeam.hasPlayer(playerId))
        .forEach(userTeam -> userTeam.setPoints(userTeam.getPoints() + delta));
  }

  private void applyTeams(
      Game game,
      String team1Name,
      List<PlayerRequest> team1Players,
      String team2Name,
      List<PlayerRequest> team2Players) {
    List<Team> existingTeams = new ArrayList<>(game.getTeams());
    if (existingTeams.size() == 2) {
      syncTeam(existingTeams.get(0), team1Name, team1Players);
      syncTeam(existingTeams.get(1), team2Name, team2Players);
    } else {
      game.getTeams().clear();
      Set<String> reservedIds = new LinkedHashSet<>();
      game.addTeam(buildTeam(team1Name, team1Players, reservedIds));
      game.addTeam(buildTeam(team2Name, team2Players, reservedIds));
    }
  }

  private Team buildTeam(String teamName, List<PlayerRequest> players, Set<String> reservedIds) {
    Team team = new Team(teamName);
    for (PlayerRequest playerRequest : players) {
      String globalUniqueId = playerIdGenerator.generateUnique(reservedIds);
      reservedIds.add(globalUniqueId);
      team.addPlayer(new Player(globalUniqueId, playerRequest.name(), playerRequest.type()));
    }
    return team;
  }

  private static void syncTeam(Team team, String teamName, List<PlayerRequest> players) {
    team.setName(teamName);
    List<Player> existingPlayers = new ArrayList<>(team.getPlayers());
    for (int index = 0; index < existingPlayers.size() && index < players.size(); index++) {
      Player player = existingPlayers.get(index);
      PlayerRequest playerRequest = players.get(index);
      player.setName(playerRequest.name());
      player.setType(playerRequest.type());
    }
  }

  private static void validateRoster(Game game) {
    List<Team> teams = new ArrayList<>(game.getTeams());
    if (teams.size() != 2) {
      throw new IllegalArgumentException("Game must have exactly two teams");
    }
    for (Team team : teams) {
      validateTeamComposition(team);
    }
    List<String> globalIds = game.getAllPlayers().stream()
        .map(Player::getGlobalUniqueId)
        .toList();
    if (new LinkedHashSet<>(globalIds).size() != globalIds.size()) {
      throw new IllegalArgumentException("Player global unique IDs must be unique");
    }
  }

  private static void validateTeamComposition(Team team) {
    if (team.getPlayers().size() != 11) {
      throw new IllegalArgumentException("Each team must contain exactly 11 players");
    }
    long wicketkeepers = team.getPlayers().stream()
        .filter(player -> player.getType() == PlayerType.WICKETKEEPER)
        .count();
    if (wicketkeepers < 1) {
      throw new IllegalArgumentException("Each team must contain at least one wicketkeeper");
    }
    long bowlersAndAllrounders = team.getPlayers().stream()
        .filter(player -> player.getType() == PlayerType.BOWLER
            || player.getType() == PlayerType.ALLROUNDER)
        .count();
    if (bowlersAndAllrounders < 5) {
      throw new IllegalArgumentException(
          "Each team must contain at least 5 bowlers and all-rounders");
    }
  }
}
