package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank String userName,
    @NotBlank @Size(min = 8, max = 128) String password) {
}
