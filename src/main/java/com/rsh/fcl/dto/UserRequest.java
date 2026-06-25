package com.rsh.fcl.dto;

import com.rsh.fcl.model.User.UserRole;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(@NotBlank String userName, String password, UserRole role) {

  public UserRole roleOrDefault() {
    return role == null ? UserRole.USER : role;
  }
}
