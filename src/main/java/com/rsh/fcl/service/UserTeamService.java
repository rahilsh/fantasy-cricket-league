package com.rsh.fcl.service;

import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.exception.UserTeamExistsException;
import com.rsh.fcl.exception.UserTeamNotFoundForGameException;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserTeamService {

  private final UserTeamRepository userTeamRepository;
  private final GameRepository gameRepository;

  public UserTeamService(UserTeamRepository userTeamRepository, GameRepository gameRepository) {
    this.userTeamRepository = userTeamRepository;
    this.gameRepository = gameRepository;
  }

  @Transactional
  public UserTeam createTeamForUser(long gameId, List<Integer> players, String userName) {
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    if (userTeamRepository.existsByGameIdAndUserName(gameId, userName)) {
      throw new UserTeamExistsException(userName, gameId);
    }
    return userTeamRepository.save(new UserTeam(game, userName, players));
  }

  @Transactional(readOnly = true)
  public List<UserTeam> getUserTeams() {
    return userTeamRepository.findAll();
  }

  @Transactional(readOnly = true)
  public UserTeam getUserTeam(long id) {
    return userTeamRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("UserTeam", id));
  }

  @Transactional(readOnly = true)
  public List<UserTeam> getUserTeamsForGame(long gameId) {
    if (!gameRepository.existsById(gameId)) {
      throw new GameNotFoundException(gameId);
    }
    List<UserTeam> userTeams = userTeamRepository.findByGameId(gameId);
    if (userTeams.isEmpty()) {
      throw new UserTeamNotFoundForGameException(gameId);
    }
    return userTeams;
  }

  @Transactional
  public UserTeam updateUserTeam(
      long id,
      long gameId,
      List<Integer> players,
      String userName,
      double points) {
    UserTeam userTeam = getUserTeam(id);
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    userTeamRepository.findByGameIdAndUserName(gameId, userName)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new UserTeamExistsException(userName, gameId);
        });

    userTeam.setGame(game);
    userTeam.setUserName(userName);
    userTeam.setPlayers(new HashSet<>(players));
    userTeam.setPoints(points);
    return userTeamRepository.save(userTeam);
  }

  @Transactional
  public void deleteUserTeam(long id) {
    userTeamRepository.delete(getUserTeam(id));
  }
}
