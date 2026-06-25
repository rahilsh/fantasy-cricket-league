package com.rsh.fcl.exception;

import com.rsh.fcl.dto.ErrorResponse;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler({GameNotFoundException.class, ResourceNotFoundException.class,
      UserTeamNotFoundForGameException.class, UserNotFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler({GameAlreadyCompletedException.class, GameNotStartedException.class,
      BallEventNotSupportedException.class, UserTeamExistsException.class,
      IllegalArgumentException.class})
  public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .orElse("Request validation failed");
    return error(HttpStatus.BAD_REQUEST, message);
  }

  private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
  }
}
