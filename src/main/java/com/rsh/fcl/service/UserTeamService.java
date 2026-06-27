package com.rsh.fcl.service;

import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.exception.UserNotFoundException;
import com.rsh.fcl.exception.UserTeamExistsException;
import com.rsh.fcl.exception.UserTeamNotFoundForGameException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserTeamService {

  private final UserTeamRepository userTeamRepository;
  private final GameRepository gameRepository;
  private final UserRepository userRepository;

  public UserTeamService(
      UserTeamRepository userTeamRepository,
      GameRepository gameRepository,
      UserRepository userRepository) {
    this.userTeamRepository = userTeamRepository;
    this.gameRepository = gameRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public UserTeam createTeamForUser(long gameId, List<String> cricketers, String userName) {
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    ensureGameIsEditable(game);
    User user = getUserByName(userName);
    if (userTeamRepository.existsByGameIdAndUser_UserName(gameId, userName)) {
      throw new UserTeamExistsException(userName, gameId);
    }
    return userTeamRepository.save(new UserTeam(game, user, resolveCricketers(game, cricketers)));
  }

  @Transactional(readOnly = true)
  public Page<UserTeam> getUserTeams(Pageable pageable) {
    return userTeamRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Page<UserTeam> getUserTeamsForUser(String userName, Pageable pageable) {
    return userTeamRepository.findByUser_UserName(userName, pageable);
  }

  @Transactional(readOnly = true)
  public UserTeam getUserTeam(long id) {
    return userTeamRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("UserTeam", id));
  }

  @Transactional(readOnly = true)
  public Page<UserTeam> getUserTeamsForGame(long gameId, Pageable pageable) {
    if (!gameRepository.existsById(gameId)) {
      throw new GameNotFoundException(gameId);
    }
    Page<UserTeam> userTeams = userTeamRepository.findByGameId(gameId, pageable);
    if (userTeams.getTotalElements() == 0) {
      throw new UserTeamNotFoundForGameException(gameId);
    }
    return userTeams;
  }

  @Transactional(readOnly = true)
  public Page<UserTeam> getUserTeamsForGameForUser(long gameId, String userName, Pageable pageable) {
    if (!gameRepository.existsById(gameId)) {
      throw new GameNotFoundException(gameId);
    }
    Page<UserTeam> userTeams = userTeamRepository.findByGameIdAndUser_UserName(gameId, userName, pageable);
    if (userTeams.getTotalElements() == 0) {
      throw new UserTeamNotFoundForGameException(gameId);
    }
    return userTeams;
  }

  @Transactional
  public UserTeam updateUserTeam(
      long id,
      long gameId,
      List<String> cricketers,
      String userName,
      double points) {
    UserTeam userTeam = getUserTeam(id);
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    ensureGameIsEditable(game);
    User user = getUserByName(userName);
    userTeamRepository.findByGameIdAndUser_UserName(gameId, userName)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new UserTeamExistsException(userName, gameId);
        });

    userTeam.setGame(game);
    userTeam.setUser(user);
    userTeam.setCricketers(resolveCricketers(game, cricketers));
    userTeam.setPoints(points);
    return userTeamRepository.save(userTeam);
  }

  @Transactional
  public void deleteUserTeam(long id) {
    UserTeam userTeam = getUserTeam(id);
    ensureGameIsEditable(userTeam.getGame());
    userTeamRepository.delete(userTeam);
  }

  @Transactional(readOnly = true)
  public UserTeam getUserTeamForUser(long id, String userName) {
    UserTeam userTeam = getUserTeam(id);
    if (!userTeam.getUserName().equals(userName)) {
      throw new AccessDeniedException("User cannot access another user's team");
    }
    return userTeam;
  }

  private User getUserByName(String userName) {
    return userRepository.findByUserName(userName)
        .orElseThrow(() -> new UserNotFoundException(userName));
  }

  private static void ensureGameIsEditable(Game game) {
    if (!game.getStatus().equals(GameStatus.CREATED)) {
      throw new IllegalArgumentException("User teams cannot be modified after game has started");
    }
  }

  private static LinkedHashSet<Cricketer> resolveCricketers(Game game, List<String> cricketerIds) {
    if (cricketerIds.size() != 11) {
      throw new IllegalArgumentException("User team must contain exactly 11 cricketers");
    }
    LinkedHashSet<Cricketer> selected = game.getAllCricketers().stream()
        .filter(cricketer -> cricketerIds.contains(cricketer.getGlobalUniqueId()))
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    if (selected.size() != 11) {
      throw new IllegalArgumentException("Selected cricketers must belong to the game roster");
    }
    if (new HashSet<>(cricketerIds).size() != 11) {
      throw new IllegalArgumentException("Cricketer global unique IDs must be unique");
    }
    long wicketkeepers = selected.stream()
        .filter(cricketer -> cricketer.getType() == CricketerType.WICKETKEEPER)
        .count();
    if (wicketkeepers < 1) {
      throw new IllegalArgumentException("User team must contain at least one wicketkeeper");
    }
    long bowlersAndAllrounders = selected.stream()
        .filter(cricketer -> cricketer.getType() == CricketerType.BOWLER
            || cricketer.getType() == CricketerType.ALLROUNDER)
        .count();
    if (bowlersAndAllrounders < 5) {
      throw new IllegalArgumentException(
          "User team must contain at least 5 bowlers and all-rounders");
    }
    return selected;
  }
}
