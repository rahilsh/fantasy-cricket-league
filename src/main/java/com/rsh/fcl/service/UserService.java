package com.rsh.fcl.service;

import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.User;
import com.rsh.fcl.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User createUser(String userName) {
    if (userRepository.existsByUserName(userName)) {
      throw new IllegalArgumentException("User already exists: " + userName);
    }
    return userRepository.save(new User(userName));
  }

  @Transactional(readOnly = true)
  public List<User> getUsers() {
    return userRepository.findAll();
  }

  @Transactional(readOnly = true)
  public User getUser(long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  @Transactional
  public User updateUser(long id, String userName) {
    userRepository.findByUserName(userName)
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new IllegalArgumentException("User already exists: " + userName);
    });
    User user = getUser(id);
    user.setUserName(userName);
    return userRepository.save(user);
  }

  @Transactional
  public void deleteUser(long id) {
    userRepository.delete(getUser(id));
  }
}
