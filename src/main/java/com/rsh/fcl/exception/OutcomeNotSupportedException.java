package com.rsh.fcl.exception;

public class OutcomeNotSupportedException extends RuntimeException {
    public OutcomeNotSupportedException(int outcome) {
        super(String.format("Outcome %d is not supported", outcome));
    }
}
