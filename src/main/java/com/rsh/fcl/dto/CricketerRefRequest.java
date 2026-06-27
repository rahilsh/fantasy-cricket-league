package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;

public record CricketerRefRequest(@NotBlank String cricketerId) {
}
