package com.rsh.fcl.exception;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String userName) {
    super(String.format("User %s does not exist", userName));
  }
}
