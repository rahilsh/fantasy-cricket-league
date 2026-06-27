package com.rsh.fcl.dto;

import java.util.List;

public record TeamResponse(
    Long id,
    String name,
    Long tournamentId,
    List<CricketerResponse> cricketers) {
}
