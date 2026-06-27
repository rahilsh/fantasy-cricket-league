package com.rsh.fcl.dto;

import com.rsh.fcl.model.PlayerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayerRequest(
    @NotNull @Positive Long globalUniqueId,
    @NotBlank String name,
    @NotNull PlayerType type) {
}
