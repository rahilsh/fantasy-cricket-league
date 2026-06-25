package com.rsh.fcl.exception;

public class GameNotStartedException extends RuntimeException {
    public GameNotStartedException(long gameId) {
        super(String.format("Game %d not yet started", gameId));
    }
}
