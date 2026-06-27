package com.rsh.fcl.exception;

public class CricketerNotFoundException extends RuntimeException {
  public CricketerNotFoundException(String cricketerId) {
    super(String.format("cricketer %s does not exist", cricketerId));
  }
}
