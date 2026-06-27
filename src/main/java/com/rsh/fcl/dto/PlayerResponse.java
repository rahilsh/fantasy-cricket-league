package com.rsh.fcl.dto;

import com.rsh.fcl.model.PlayerType;

public record PlayerResponse(Long globalUniqueId, String name, PlayerType type) {
}
