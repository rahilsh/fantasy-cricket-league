package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotNull;

public record OutcomeRequest(@NotNull Long gameId, int batsman, int bowler, int score) {
}
