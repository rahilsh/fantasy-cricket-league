package com.rsh.fcl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GameRequest(
    @NotBlank String team1,
    @NotBlank String team2,
    @Min(1) Integer k) {

  public int topKOrDefault() {
    return k == null ? 3 : k;
  }
}
