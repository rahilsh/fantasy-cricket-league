package com.rsh.fcl.controller;

import com.rsh.fcl.service.UserTeamService;
import com.rsh.fcl.dto.UserTeamRequest;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.mapper.DtoMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-teams")
public class UserTeamController {

  private final UserTeamService userTeamService;

  public UserTeamController(UserTeamService userTeamService) {
    this.userTeamService = userTeamService;
  }

  @PostMapping
  public ResponseEntity<UserTeamResponse> createUserTeam(
      @Valid @RequestBody UserTeamRequest request) {
    UserTeamResponse response = DtoMapper.toUserTeamResponse(
        userTeamService.createTeamForUser(request.gameId(), request.players(), request.userName()));
    return ResponseEntity.created(URI.create("/api/user-teams/" + response.id())).body(response);
  }

  @GetMapping
  public List<UserTeamResponse> getUserTeams(@RequestParam(required = false) Long gameId) {
    if (gameId != null) {
      return userTeamService.getUserTeamsForGame(gameId).stream()
          .map(DtoMapper::toUserTeamResponse)
          .toList();
    }
    return userTeamService.getUserTeams().stream()
        .map(DtoMapper::toUserTeamResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public UserTeamResponse getUserTeam(@PathVariable long id) {
    return DtoMapper.toUserTeamResponse(userTeamService.getUserTeam(id));
  }

  @PutMapping("/{id}")
  public UserTeamResponse updateUserTeam(
      @PathVariable long id,
      @Valid @RequestBody UserTeamRequest request) {
    return DtoMapper.toUserTeamResponse(userTeamService.updateUserTeam(id, request.gameId(),
        request.players(), request.userName(), request.pointsOrDefault()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUserTeam(@PathVariable long id) {
    userTeamService.deleteUserTeam(id);
    return ResponseEntity.noContent().build();
  }
}
