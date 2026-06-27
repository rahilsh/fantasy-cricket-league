package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;

public record TournamentRequest(@NotBlank String name) {
}
