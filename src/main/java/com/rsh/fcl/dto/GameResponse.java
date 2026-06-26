package com.rsh.fcl.dto;

import com.rsh.fcl.model.Game.GameStatus;

public record GameResponse(Long id, String team1, String team2, GameStatus status, int k,
    int overs, int ballsBowled) {
}
