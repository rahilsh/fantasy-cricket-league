package com.rsh.fcl.exception;

public class BallEventNotSupportedException extends RuntimeException {
  public BallEventNotSupportedException(int outcome) {
    super(String.format("Ball event outcome %d is not supported", outcome));
  }
}
