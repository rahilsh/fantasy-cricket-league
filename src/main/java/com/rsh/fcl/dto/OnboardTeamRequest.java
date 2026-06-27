package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OnboardTeamRequest(
    @NotBlank String name,
    @NotEmpty @Size(min = 11, max = 11, message = "must contain exactly 11 cricketers")
    List<@NotBlank String> cricketers) {
}
