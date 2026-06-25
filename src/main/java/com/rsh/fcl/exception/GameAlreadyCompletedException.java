package com.rsh.fcl.exception;

public class GameAlreadyCompletedException extends RuntimeException {
    public GameAlreadyCompletedException(long gameId) {
        super(String.format("Game %d already completed", gameId));
    }
}
