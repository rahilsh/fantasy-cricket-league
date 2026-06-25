package com.rsh.fcl.service;

import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.exception.UserNotFoundException;
import com.rsh.fcl.exception.UserTeamExistsException;
import com.rsh.fcl.exception.UserTeamNotFoundForGameException;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.HashSet;
import java.util.List;
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
  public UserTeam createTeamForUser(long gameId, List<Integer> players, String userName) {
    Game game = gameRepository.findById(gameId)
        .orElseThrow(() -> new GameNotFoundException(gameId));
    User user = getUserByName(userName);
    if (userTeamRepository.existsByGameIdAndUser_UserName(gameId, userName)) {
      throw new UserTeamExistsException(userName, gameId);
    }
    return userTeamRepository.save(new UserTeam(game, user, players));
  }

  @Transactional(readOnly = true)
  public Page<UserTeam> getUserTeams(Pageable pageable) {
    return userTeamRepository.findAll(pageable);
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
    User user = getUserByName(userName);
    userTeamRepository.findByGameIdAndUser_UserName(gameId, userName)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new UserTeamExistsException(userName, gameId);
        });

    userTeam.setGame(game);
    userTeam.setUser(user);
    userTeam.setPlayers(new HashSet<>(players));
    userTeam.setPoints(points);
    return userTeamRepository.save(userTeam);
  }

  @Transactional
  public void deleteUserTeam(long id) {
    userTeamRepository.delete(getUserTeam(id));
  }

  private User getUserByName(String userName) {
    return userRepository.findByUserName(userName)
        .orElseThrow(() -> new UserNotFoundException(userName));
  }
}
