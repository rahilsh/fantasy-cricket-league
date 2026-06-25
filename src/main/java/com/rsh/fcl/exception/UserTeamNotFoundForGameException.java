package com.rsh.fcl.exception;

public class UserTeamNotFoundForGameException extends RuntimeException {
    public UserTeamNotFoundForGameException(long gameId) {
        super(String.format("UserTeam does not exists for game %d", gameId));
    }
}
