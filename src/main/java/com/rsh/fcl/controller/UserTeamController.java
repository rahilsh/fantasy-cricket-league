package com.rsh.fcl.controller;

import com.rsh.fcl.service.UserTeamService;
import com.rsh.fcl.dto.UserTeamRequest;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.mapper.DtoMapper;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
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
      @Valid @RequestBody UserTeamRequest request,
      Authentication authentication) {
    String effectiveUserName = isAdmin(authentication) ? request.userName() : authentication.getName();
    UserTeamResponse response = DtoMapper.toUserTeamResponse(
        userTeamService.createTeamForUser(request.gameId(), request.players(), effectiveUserName));
    return ResponseEntity.created(URI.create("/api/user-teams/" + response.id())).body(response);
  }

  @GetMapping
  public Page<UserTeamResponse> getUserTeams(
      @RequestParam(required = false) Long gameId,
      @PageableDefault(size = 20, sort = "id") Pageable pageable,
      Authentication authentication) {
    if (!isAdmin(authentication)) {
      if (gameId != null) {
        return userTeamService.getUserTeamsForGameForUser(gameId, authentication.getName(), pageable)
            .map(DtoMapper::toUserTeamResponse);
      }
      return userTeamService.getUserTeamsForUser(authentication.getName(), pageable)
          .map(DtoMapper::toUserTeamResponse);
    }
    if (gameId != null) {
      return userTeamService.getUserTeamsForGame(gameId, pageable).map(DtoMapper::toUserTeamResponse);
    }
    return userTeamService.getUserTeams(pageable).map(DtoMapper::toUserTeamResponse);
  }

  @GetMapping("/{id}")
  public UserTeamResponse getUserTeam(@PathVariable long id, Authentication authentication) {
    if (isAdmin(authentication)) {
      return DtoMapper.toUserTeamResponse(userTeamService.getUserTeam(id));
    }
    return DtoMapper.toUserTeamResponse(userTeamService.getUserTeamForUser(id, authentication.getName()));
  }

  @PutMapping("/{id}")
  public UserTeamResponse updateUserTeam(
      @PathVariable long id,
      @Valid @RequestBody UserTeamRequest request,
      Authentication authentication) {
    String effectiveUserName = isAdmin(authentication) ? request.userName() : authentication.getName();
    double effectivePoints = isAdmin(authentication)
        ? request.pointsOrDefault()
        : userTeamService.getUserTeamForUser(id, effectiveUserName).getPoints();
    return DtoMapper.toUserTeamResponse(userTeamService.updateUserTeam(id, request.gameId(),
        request.players(), effectiveUserName, effectivePoints));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUserTeam(@PathVariable long id, Authentication authentication) {
    if (!isAdmin(authentication)) {
      userTeamService.getUserTeamForUser(id, authentication.getName());
    }
    userTeamService.deleteUserTeam(id);
    return ResponseEntity.noContent().build();
  }

  private static boolean isAdmin(Authentication authentication) {
    if (authentication == null) {
      return true;
    }
    return authentication.getAuthorities().stream()
        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
  }
}
