package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayRequest(
    @NotNull @Positive Long batsman,
    @NotNull @Positive Long bowler,
    @NotNull Integer outcome) {
}
