package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayRequest(
    @NotBlank String batsman,
    @NotBlank String bowler,
    @NotNull Integer outcome) {
}
