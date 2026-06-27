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
@RequestMapping("/api/games")
public class GameController {

  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  @PostMapping
  public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameRequest request) {
    GameResponse response = DtoMapper.toGameResponse(
        gameService.createGame(request.team1(), request.team2(), request.topKOrDefault(),
            request.overs(), request.team1Players(), request.team2Players()));
    return ResponseEntity.created(URI.create("/api/games/" + response.id())).body(response);
  }

  @GetMapping
  public Page<GameResponse> getGames(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
    return gameService.getGames(pageable).map(DtoMapper::toGameResponse);
  }

  @GetMapping("/{id}")
  public GameResponse getGame(@PathVariable long id) {
    return DtoMapper.toGameResponse(gameService.getGame(id));
  }

  @PutMapping("/{id}")
  public GameResponse updateGame(@PathVariable long id, @Valid @RequestBody GameRequest request) {
    return DtoMapper.toGameResponse(
        gameService.updateGame(id, request.team1(), request.team2(), request.topKOrDefault(),
            request.overs(), request.team1Players(), request.team2Players()));
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
  public BallEventResponse play(@PathVariable long id, @Valid @RequestBody PlayRequest request) {
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
