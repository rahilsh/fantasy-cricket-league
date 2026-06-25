package com.rsh.fcl.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRequest(@NotBlank String userName) {
}
