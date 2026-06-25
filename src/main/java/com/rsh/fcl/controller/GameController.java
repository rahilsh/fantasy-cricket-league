package com.rsh.fcl.controller;

import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.service.GameService;
import com.rsh.fcl.dto.GameRequest;
import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.PlayRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @PostMapping
  public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameRequest request) {
    GameResponse response = DtoMapper.toGameResponse(
        gameService.createGame(request.team1(), request.team2(), request.topKOrDefault()));
    return ResponseEntity.created(URI.create("/api/games/" + response.id())).body(response);
  }

  @GetMapping
  public List<GameResponse> getGames() {
    return gameService.getGames().stream()
        .map(DtoMapper::toGameResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public GameResponse getGame(@PathVariable long id) {
    return DtoMapper.toGameResponse(gameService.getGame(id));
  }

  @PutMapping("/{id}")
  public GameResponse updateGame(@PathVariable long id, @Valid @RequestBody GameRequest request) {
    return DtoMapper.toGameResponse(
        gameService.updateGame(id, request.team1(), request.team2(), request.topKOrDefault()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteGame(@PathVariable long id) {
    gameService.deleteGame(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/start")
  public GameResponse startGame(@PathVariable long id) {
    return DtoMapper.toGameResponse(gameService.startGame(id));
  }

  @PostMapping("/{id}/end")
  public GameResponse endGame(@PathVariable long id) {
    return DtoMapper.toGameResponse(gameService.endGame(id));
  }

  @PostMapping("/{id}/plays")
  public BallEventResponse play(@PathVariable long id, @RequestBody PlayRequest request) {
    BallEvent ballEvent = gameService.play(id, request.batsman(), request.bowler(), request.outcome());
    return DtoMapper.toBallEventResponse(ballEvent);
  }

  @GetMapping("/{id}/leaderboard")
  public List<LeaderboardEntry> getLeaderboard(@PathVariable long id) {
    return gameService.getLeaderboard(id).stream()
        .map(DtoMapper::toLeaderboardEntry)
        .toList();
  }
}
