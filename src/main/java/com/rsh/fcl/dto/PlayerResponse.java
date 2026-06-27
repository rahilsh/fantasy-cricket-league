package com.rsh.fcl.dto;

import com.rsh.fcl.model.PlayerType;

public record PlayerResponse(String globalUniqueId, String name, PlayerType type) {
}
