package com.rsh.fcl.controller;

import com.rsh.fcl.service.OutcomeService;
import com.rsh.fcl.dto.OutcomeRequest;
import com.rsh.fcl.dto.OutcomeResponse;
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
@RequestMapping("/api/outcomes")
public class OutcomeController {

  private final OutcomeService outcomeService;

  public OutcomeController(OutcomeService outcomeService) {
    this.outcomeService = outcomeService;
  }

  @PostMapping
  public ResponseEntity<OutcomeResponse> createOutcome(@Valid @RequestBody OutcomeRequest request) {
    OutcomeResponse response = DtoMapper.toOutcomeResponse(outcomeService.createOutcome(
        request.gameId(), request.batsman(), request.bowler(), request.score()));
    return ResponseEntity.created(URI.create("/api/outcomes/" + response.id())).body(response);
  }

  @GetMapping
  public List<OutcomeResponse> getOutcomes(@RequestParam(required = false) Long gameId) {
    if (gameId != null) {
      return outcomeService.getOutcomesForGame(gameId).stream()
          .map(DtoMapper::toOutcomeResponse)
          .toList();
    }
    return outcomeService.getOutcomes().stream()
        .map(DtoMapper::toOutcomeResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public OutcomeResponse getOutcome(@PathVariable long id) {
    return DtoMapper.toOutcomeResponse(outcomeService.getOutcome(id));
  }

  @PutMapping("/{id}")
  public OutcomeResponse updateOutcome(
      @PathVariable long id,
      @Valid @RequestBody OutcomeRequest request) {
    return DtoMapper.toOutcomeResponse(outcomeService.updateOutcome(id, request.gameId(),
        request.batsman(), request.bowler(), request.score()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOutcome(@PathVariable long id) {
    outcomeService.deleteOutcome(id);
    return ResponseEntity.noContent().build();
  }
}
