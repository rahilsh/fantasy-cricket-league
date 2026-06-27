package com.rsh.fcl.dto;

public record BallEventResponse(Long id, Long gameId, String batsman, String bowler, int score) {
}
