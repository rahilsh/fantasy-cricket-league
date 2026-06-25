package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayRequest(
    @NotNull @Positive Integer batsman,
    @NotNull @Positive Integer bowler,
    @NotNull Integer outcome) {
}
