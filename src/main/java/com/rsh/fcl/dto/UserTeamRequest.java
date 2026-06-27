package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserTeamRequest(
    @NotNull @Positive Long gameId,
    @NotBlank String userName,
    @NotEmpty @Size(min = 11, max = 11, message = "must contain exactly 11 cricketers")
    List<@NotBlank String> cricketers,
    Double points) {

  public double pointsOrDefault() {
    return points == null ? 0.0 : points;
  }
}
