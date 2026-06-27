package com.rsh.fcl.controller;

import com.rsh.fcl.dto.CricketerRefRequest;
import com.rsh.fcl.dto.TeamResponse;
import com.rsh.fcl.mapper.DtoMapper;
import com.rsh.fcl.service.TeamService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/teams")
public class TeamController {

  private final TeamService teamService;

  public TeamController(TeamService teamService) {
    this.teamService = teamService;
  }

  @GetMapping
  public Page<TeamResponse> getTeams(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return teamService.getTeams(pageable).map(DtoMapper::toTeamResponse);
  }

  @GetMapping("/{id}")
  public TeamResponse getTeam(@PathVariable long id) {
    return DtoMapper.toTeamResponse(teamService.getTeam(id));
  }

  @PostMapping("/{id}/cricketers")
  public TeamResponse addCricketer(
      @PathVariable long id,
      @Valid @RequestBody CricketerRefRequest request) {
    return DtoMapper.toTeamResponse(teamService.addCricketer(id, request.cricketerId()));
  }

  @PutMapping("/{id}/cricketers/{cricketerId}")
  public TeamResponse replaceCricketer(
      @PathVariable long id,
      @PathVariable String cricketerId,
      @Valid @RequestBody CricketerRefRequest request) {
    return DtoMapper.toTeamResponse(
        teamService.replaceCricketer(id, cricketerId, request.cricketerId()));
  }

  @DeleteMapping("/{id}/cricketers/{cricketerId}")
  public ResponseEntity<Void> removeCricketer(
      @PathVariable long id,
      @PathVariable String cricketerId) {
    teamService.removeCricketer(id, cricketerId);
    return ResponseEntity.noContent().build();
  }
}
