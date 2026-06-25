package com.rsh.fcl.exception;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String resource, long id) {
    super(String.format("%s %d does not exist", resource, id));
  }
}
