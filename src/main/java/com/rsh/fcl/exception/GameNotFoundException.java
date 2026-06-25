package com.rsh.fcl.exception;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(long gameId) {
        super(String.format("gameId %d does not exist", gameId));
    }
}
