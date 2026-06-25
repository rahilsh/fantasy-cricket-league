package com.rsh.fcl.exception;

public class UserTeamExistsException extends RuntimeException {
    public UserTeamExistsException(String userName, long gameId) {
        super(String.format("User team for user %s already for game %s", userName, gameId));
    }
}
