package com.rsh.fcl.controller;

import com.rsh.fcl.dto.OnboardTeamRequest;
import com.rsh.fcl.dto.TeamResponse;
import com.rsh.fcl.dto.TournamentRequest;
import com.rsh.fcl.dto.TournamentResponse;
import com.rsh.fcl.mapper.DtoMapper;
import com.rsh.fcl.service.TournamentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

  private final TournamentService tournamentService;

  public TournamentController(TournamentService tournamentService) {
    this.tournamentService = tournamentService;
  }

  @PostMapping
  public ResponseEntity<TournamentResponse> createTournament(
      @Valid @RequestBody TournamentRequest request) {
    TournamentResponse response = DtoMapper.toTournamentResponse(
        tournamentService.createTournament(request.name()));
    return ResponseEntity.created(URI.create("/api/tournaments/" + response.id())).body(response);
  }

  @GetMapping
  public Page<TournamentResponse> getTournaments(
      @PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return tournamentService.getTournaments(pageable).map(DtoMapper::toTournamentResponse);
  }

  @GetMapping("/{id}")
  public TournamentResponse getTournament(@PathVariable long id) {
    return DtoMapper.toTournamentResponse(tournamentService.getTournament(id));
  }

  @PutMapping("/{id}")
  public TournamentResponse updateTournament(
      @PathVariable long id,
      @Valid @RequestBody TournamentRequest request) {
    return DtoMapper.toTournamentResponse(tournamentService.updateTournament(id, request.name()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTournament(@PathVariable long id) {
    tournamentService.deleteTournament(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/start")
  public TournamentResponse startTournament(@PathVariable long id) {
    return DtoMapper.toTournamentResponse(tournamentService.startTournament(id));
  }

  @PostMapping("/{id}/end")
  public TournamentResponse endTournament(@PathVariable long id) {
    return DtoMapper.toTournamentResponse(tournamentService.endTournament(id));
  }

  @PostMapping("/{id}/teams")
  public ResponseEntity<TeamResponse> onboardTeam(
      @PathVariable long id,
      @Valid @RequestBody OnboardTeamRequest request) {
    TeamResponse response = DtoMapper.toTeamResponse(
        tournamentService.onboardTeam(id, request.name(), request.cricketers()));
    return ResponseEntity.created(URI.create("/api/teams/" + response.id())).body(response);
  }
}
