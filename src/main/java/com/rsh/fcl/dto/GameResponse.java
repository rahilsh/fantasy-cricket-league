package com.rsh.fcl.dto;

import com.rsh.fcl.model.Game.GameStatus;
import java.util.List;

public record GameResponse(
    Long id,
    Long tournamentId,
    Long team1Id,
    Long team2Id,
    String team1,
    String team2,
    GameStatus status,
    int k,
    int overs,
    int ballsBowled,
    int wickets,
    List<CricketerResponse> team1Cricketers,
    List<CricketerResponse> team2Cricketers) {
}
