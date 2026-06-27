package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rsh.fcl.dto.AuthResponse;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.User.UserRole;
import com.rsh.fcl.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String SUPERADMIN_USER = "fcl-admin";
  private static final String SUPERADMIN_PASS = "fcl-admin-password";

  @Mock
  private UserRepository userRepository;
  @Mock
  private UserService userService;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtTokenService jwtTokenService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, userService, passwordEncoder, jwtTokenService,
        SUPERADMIN_USER, SUPERADMIN_PASS);
  }

  private void stubToken(String userName, UserRole role) {
    when(jwtTokenService.generateAccessToken(userName, role)).thenReturn("token-" + role);
    when(jwtTokenService.getAccessTokenTtlSeconds()).thenReturn(3600L);
  }

  @Test
  void signupCreatesUserAndReturnsToken() {
    User created = new User("alice", "hash", UserRole.USER);
    when(userService.createUser("alice", "pw", UserRole.USER)).thenReturn(created);
    stubToken("alice", UserRole.USER);

    AuthResponse response = authService.signup("alice", "pw");

    assertThat(response.accessToken()).isEqualTo("token-USER");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.role()).isEqualTo("USER");
    assertThat(response.expiresInSeconds()).isEqualTo(3600L);
  }

  @Test
  void loginAsSuperadminUsesConfiguredCredentials() {
    stubToken(SUPERADMIN_USER, UserRole.SUPERADMIN);

    AuthResponse response = authService.login(SUPERADMIN_USER, SUPERADMIN_PASS);

    assertThat(response.role()).isEqualTo("SUPERADMIN");
  }

  @Test
  void loginAsUserChecksPassword() {
    User user = new User("alice", "hash", UserRole.USER);
    when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
    stubToken("alice", UserRole.USER);

    AuthResponse response = authService.login("alice", "pw");

    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void loginRejectsUnknownUser() {
    when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> authService.login("ghost", "pw"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid credentials");
  }

  @Test
  void loginRejectsWrongPassword() {
    User user = new User("alice", "hash", UserRole.USER);
    when(userRepository.findByUserName("alice")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("bad", "hash")).thenReturn(false);
    assertThatThrownBy(() -> authService.login("alice", "bad"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid credentials");
  }
}
