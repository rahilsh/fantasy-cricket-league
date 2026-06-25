package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UserTeamRequest(
    @NotNull @Positive Long gameId,
    @NotBlank String userName,
    @NotEmpty List<@NotNull @Positive Integer> players,
    Double points) {

  public double pointsOrDefault() {
    return points == null ? 0.0 : points;
  }
}
