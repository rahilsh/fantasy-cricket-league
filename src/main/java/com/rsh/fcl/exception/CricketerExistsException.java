package com.rsh.fcl.exception;

public class CricketerExistsException extends RuntimeException {
  public CricketerExistsException(String cricketerId) {
    super(String.format("cricketer %s already exists", cricketerId));
  }
}
