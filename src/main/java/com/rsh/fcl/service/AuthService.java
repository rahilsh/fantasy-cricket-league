package com.rsh.fcl.service;

import com.rsh.fcl.dto.AuthResponse;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.User.UserRole;
import com.rsh.fcl.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final String superadminUserName;
  private final String superadminPassword;

  public AuthService(
      UserRepository userRepository,
      UserService userService,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService,
      @Value("${fcl.security.superadmin.username:${FCL_SECURITY_SUPERADMIN_USERNAME:fcl-admin}}")
      String superadminUserName,
      @Value("${fcl.security.superadmin.password:${FCL_SECURITY_SUPERADMIN_PASSWORD:fcl-admin-password}}")
      String superadminPassword) {
    this.userRepository = userRepository;
    this.userService = userService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.superadminUserName = superadminUserName;
    this.superadminPassword = superadminPassword;
  }

  @Transactional
  public AuthResponse signup(String userName, String password) {
    User createdUser = userService.createUser(userName, password, UserRole.USER);
    return tokenFor(createdUser.getUserName(), createdUser.getRole());
  }

  @Transactional(readOnly = true)
  public AuthResponse login(String userName, String password) {
    if (superadminUserName.equals(userName) && superadminPassword.equals(password)) {
      return tokenFor(userName, UserRole.SUPERADMIN);
    }

    User user = userRepository.findByUserName(userName)
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid credentials");
    }
    return tokenFor(user.getUserName(), user.getRole());
  }

  private AuthResponse tokenFor(String userName, UserRole role) {
    String token = jwtTokenService.generateAccessToken(userName, role);
    return new AuthResponse(token, "Bearer", jwtTokenService.getAccessTokenTtlSeconds(), role.name());
  }
}
