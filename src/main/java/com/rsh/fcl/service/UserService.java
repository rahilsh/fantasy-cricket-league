package com.rsh.fcl.service;

import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.User.UserRole;
import com.rsh.fcl.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String superadminUserName;

  public UserService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @org.springframework.beans.factory.annotation.Value("${fcl.security.superadmin.username:${FCL_SECURITY_SUPERADMIN_USERNAME:fcl-admin}}")
      String superadminUserName) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.superadminUserName = superadminUserName;
  }

  @Transactional
  public User createUser(String userName) {
    return createUser(userName, null, UserRole.USER);
  }

  @Transactional
  public User createUser(String userName, String rawPassword, UserRole role) {
    ensureUserNameIsAvailable(userName);
    if (role == UserRole.SUPERADMIN) {
      throw new IllegalArgumentException("Superadmin users are managed through configuration");
    }
    String effectivePassword = rawPassword;
    if (effectivePassword == null || effectivePassword.isBlank()) {
      effectivePassword = UUID.randomUUID().toString();
    }
    return userRepository.save(new User(userName, passwordEncoder.encode(effectivePassword), role));
  }

  private void ensureUserNameIsAvailable(String userName) {
    if (superadminUserName.equals(userName)) {
      throw new IllegalArgumentException("User name is reserved: " + userName);
    }
    if (userRepository.existsByUserName(userName)) {
      throw new IllegalArgumentException("User already exists: " + userName);
    }
  }

  @Transactional(readOnly = true)
  public Page<User> getUsers(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  @Transactional(readOnly = true)
  public User getUser(long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
  }

  @Transactional
  public User updateUser(long id, String userName) {
    if (superadminUserName.equals(userName)) {
      throw new IllegalArgumentException("User name is reserved: " + userName);
    }
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
