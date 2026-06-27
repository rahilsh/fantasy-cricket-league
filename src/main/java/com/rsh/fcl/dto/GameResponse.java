package com.rsh.fcl.dto;

import com.rsh.fcl.model.Game.GameStatus;
import java.util.List;

public record GameResponse(Long id, String team1, String team2, GameStatus status, int k,
    int overs, int ballsBowled, int wickets, List<PlayerResponse> team1Players,
    List<PlayerResponse> team2Players) {
}
