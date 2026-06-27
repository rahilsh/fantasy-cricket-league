package com.rsh.fcl.dto;

import java.util.Set;

public record UserTeamResponse(
    Long id,
    Long gameId,
    String userName,
    double points,
    Set<Long> players) {
}
