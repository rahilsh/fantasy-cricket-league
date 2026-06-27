package com.rsh.fcl.dto;

import com.rsh.fcl.model.PlayerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayerRequest(
    @NotBlank String name,
    @NotNull PlayerType type) {
}
