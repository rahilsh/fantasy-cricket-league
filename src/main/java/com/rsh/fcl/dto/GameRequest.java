package com.rsh.fcl.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GameRequest(
    @NotBlank String team1,
    @NotBlank String team2,
    @Min(1) Integer k,
    @NotNull @Min(1) Integer overs,
    @NotNull @Size(min = 11, max = 11, message = "must contain exactly 11 players")
    List<@Valid PlayerRequest> team1Players,
    @NotNull @Size(min = 11, max = 11, message = "must contain exactly 11 players")
    List<@Valid PlayerRequest> team2Players) {

  public int topKOrDefault() {
    return k == null ? 3 : k;
  }
}
