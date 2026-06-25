package com.rsh.fcl.controller;

import com.rsh.fcl.service.UserService;
import com.rsh.fcl.dto.UserRequest;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.mapper.DtoMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
    UserResponse response = DtoMapper.toUserResponse(userService.createUser(request.userName()));
    return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
  }

  @GetMapping
  public List<UserResponse> getUsers() {
    return userService.getUsers().stream()
        .map(DtoMapper::toUserResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public UserResponse getUser(@PathVariable long id) {
    return DtoMapper.toUserResponse(userService.getUser(id));
  }

  @PutMapping("/{id}")
  public UserResponse updateUser(@PathVariable long id, @Valid @RequestBody UserRequest request) {
    return DtoMapper.toUserResponse(userService.updateUser(id, request.userName()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}
