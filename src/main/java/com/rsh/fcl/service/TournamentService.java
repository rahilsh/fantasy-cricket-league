package com.rsh.fcl.service;

import com.rsh.fcl.exception.CricketerNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import com.rsh.fcl.repository.CricketerRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.repository.TournamentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {

  private final TournamentRepository tournamentRepository;
  private final TeamRepository teamRepository;
  private final CricketerRepository cricketerRepository;

  public TournamentService(
      TournamentRepository tournamentRepository,
      TeamRepository teamRepository,
      CricketerRepository cricketerRepository) {
    this.tournamentRepository = tournamentRepository;
    this.teamRepository = teamRepository;
    this.cricketerRepository = cricketerRepository;
  }

  @Transactional
  public Tournament createTournament(String name) {
    return tournamentRepository.save(new Tournament(name));
  }

  @Transactional(readOnly = true)
  public Page<Tournament> getTournaments(Pageable pageable) {
    return tournamentRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public Tournament getTournament(long tournamentId) {
    return findTournament(tournamentId);
  }

  @Transactional
  public Tournament updateTournament(long tournamentId, String name) {
    Tournament tournament = findTournament(tournamentId);
    tournament.setName(name);
    return tournamentRepository.save(tournament);
  }

  @Transactional
  public void deleteTournament(long tournamentId) {
    tournamentRepository.delete(findTournament(tournamentId));
  }

  @Transactional
  public Tournament startTournament(long tournamentId) {
    Tournament tournament = findTournament(tournamentId);
    if (tournament.getStatus() == TournamentStatus.COMPLETED) {
      throw new IllegalArgumentException("Completed tournament cannot be started");
    }
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    return tournamentRepository.save(tournament);
  }

  @Transactional
  public Tournament endTournament(long tournamentId) {
    Tournament tournament = findTournament(tournamentId);
    tournament.setStatus(TournamentStatus.COMPLETED);
    return tournamentRepository.save(tournament);
  }

  @Transactional
  public Team onboardTeam(long tournamentId, String teamName, List<String> cricketerIds) {
    Tournament tournament = findTournament(tournamentId);
    if (tournament.getStatus() == TournamentStatus.COMPLETED) {
      throw new IllegalArgumentException("Cannot onboard teams to a completed tournament");
    }
    LinkedHashSet<Cricketer> cricketers = resolveCricketers(cricketerIds);
    SquadRules.validateComposition(cricketers, "Team");
    Team team = new Team(teamName);
    cricketers.forEach(team::addCricketer);
    tournament.addTeam(team);
    return teamRepository.save(team);
  }

  private LinkedHashSet<Cricketer> resolveCricketers(List<String> cricketerIds) {
    if (new LinkedHashSet<>(cricketerIds).size() != cricketerIds.size()) {
      throw new IllegalArgumentException("Cricketer ids must be unique");
    }
    LinkedHashSet<Cricketer> cricketers = new LinkedHashSet<>();
    for (String cricketerId : cricketerIds) {
      Cricketer cricketer = cricketerRepository.findById(cricketerId)
          .orElseThrow(() -> new CricketerNotFoundException(cricketerId));
      ensureNotInActiveTeam(cricketer);
      cricketers.add(cricketer);
    }
    return cricketers;
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

  private Tournament findTournament(long tournamentId) {
    return tournamentRepository.findById(tournamentId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament", tournamentId));
  }
}
