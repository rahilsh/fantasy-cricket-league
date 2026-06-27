package com.rsh.fcl.dto;

import com.rsh.fcl.model.Tournament.TournamentStatus;
import java.util.List;

public record TournamentResponse(
    Long id,
    String name,
    TournamentStatus status,
    List<TeamResponse> teams) {
}
