package com.rsh.fcl.dto;

import com.rsh.fcl.model.CricketerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CricketerRequest(
    @NotBlank String name,
    @NotNull CricketerType type) {
}
