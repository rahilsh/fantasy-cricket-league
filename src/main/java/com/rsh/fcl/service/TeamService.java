package com.rsh.fcl.service;

import com.rsh.fcl.exception.CricketerNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import com.rsh.fcl.repository.CricketerRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

  private final TeamRepository teamRepository;
  private final CricketerRepository cricketerRepository;
  private final GameRepository gameRepository;

  public TeamService(
      TeamRepository teamRepository,
      CricketerRepository cricketerRepository,
      GameRepository gameRepository) {
    this.teamRepository = teamRepository;
    this.cricketerRepository = cricketerRepository;
    this.gameRepository = gameRepository;
  }

  @Transactional(readOnly = true)
  public Page<Team> getTeams(Pageable pageable) {
    return teamRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Team getTeam(long teamId) {
    return findTeam(teamId);
  }

  @Transactional
  public Team addCricketer(long teamId, String cricketerId) {
    Team team = findTeam(teamId);
    ensureEditable(team);
    if (team.getCricketers().size() >= SquadRules.SQUAD_SIZE) {
      throw new IllegalArgumentException("Team already has 11 cricketers");
    }
    if (team.hasCricketer(cricketerId)) {
      throw new IllegalArgumentException("Cricketer " + cricketerId + " is already in the team");
    }
    Cricketer cricketer = findCricketer(cricketerId);
    ensureNotInActiveTeam(cricketer);
    team.addCricketer(cricketer);
    return teamRepository.save(team);
  }

  @Transactional
  public Team replaceCricketer(long teamId, String outgoingId, String incomingId) {
    Team team = findTeam(teamId);
    ensureEditable(team);
    if (!team.hasCricketer(outgoingId)) {
      throw new IllegalArgumentException("Cricketer " + outgoingId + " is not in the team");
    }
    if (team.hasCricketer(incomingId)) {
      throw new IllegalArgumentException("Cricketer " + incomingId + " is already in the team");
    }
    Cricketer incoming = findCricketer(incomingId);
    ensureNotInActiveTeam(incoming);
    team.getCricketers().removeIf(c -> c.getGlobalUniqueId().equals(outgoingId));
    team.addCricketer(incoming);
    SquadRules.validateComposition(team.getCricketers(), "Team");
    return teamRepository.save(team);
  }

  @Transactional
  public Team removeCricketer(long teamId, String cricketerId) {
    Team team = findTeam(teamId);
    ensureEditable(team);
    if (!team.hasCricketer(cricketerId)) {
      throw new IllegalArgumentException("Cricketer " + cricketerId + " is not in the team");
    }
    team.getCricketers().removeIf(c -> c.getGlobalUniqueId().equals(cricketerId));
    return teamRepository.save(team);
  }

  private void ensureEditable(Team team) {
    if (team.getTournament().getStatus() != TournamentStatus.IN_PROGRESS) {
      throw new IllegalArgumentException(
          "Team cricketers can only be changed while the tournament is in progress");
    }
    boolean inLiveGame = !gameRepository
        .findByStatusAndTeam(GameStatus.IN_PROGRESS, team.getId()).isEmpty();
    if (inLiveGame) {
      throw new IllegalArgumentException(
          "Team cricketers cannot be changed while a match is in progress");
    }
  }

  private void ensureNotInActiveTeam(Cricketer cricketer) {
    boolean engaged = !teamRepository
        .findActiveTeamsForCricketer(cricketer.getGlobalUniqueId(), TournamentStatus.COMPLETED)
        .isEmpty();
    if (engaged) {
      throw new IllegalArgumentException(
          "Cricketer " + cricketer.getGlobalUniqueId()
              + " is already part of another active tournament");
    }
  }

  private Team findTeam(long teamId) {
    return teamRepository.findById(teamId)
        .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
  }

  private Cricketer findCricketer(String cricketerId) {
    return cricketerRepository.findById(cricketerId)
        .orElseThrow(() -> new CricketerNotFoundException(cricketerId));
  }
}
