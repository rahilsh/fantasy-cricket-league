package com.rsh.fcl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GameRequest(
    @NotNull Long tournamentId,
    @NotNull Long team1Id,
    @NotNull Long team2Id,
    @Min(1) Integer k,
    @NotNull @Min(1) Integer overs) {

  public int topKOrDefault() {
    return k == null ? 3 : k;
  }
}
