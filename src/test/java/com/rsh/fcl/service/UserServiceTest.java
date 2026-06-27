package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.User.UserRole;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final String SUPERADMIN = "fcl-admin";

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, passwordEncoder, SUPERADMIN);
  }

  @Test
  void createUserHashesPasswordAndPersists() {
    when(userRepository.existsByUserName("alice")).thenReturn(false);
    when(passwordEncoder.encode("secret")).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    User user = userService.createUser("alice", "secret", UserRole.USER);

    assertThat(user.getUserName()).isEqualTo("alice");
    assertThat(user.getPasswordHash()).isEqualTo("hashed");
    assertThat(user.getRole()).isEqualTo(UserRole.USER);
  }

  @Test
  void createUserGeneratesPasswordWhenBlank() {
    when(userRepository.existsByUserName("alice")).thenReturn(false);
    when(passwordEncoder.encode(any())).thenReturn("hashed");
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    userService.createUser("alice");

    verify(passwordEncoder).encode(any(String.class));
  }

  @Test
  void createUserRejectsReservedName() {
    assertThatThrownBy(() -> userService.createUser(SUPERADMIN, "x", UserRole.USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reserved");
  }

  @Test
  void createUserRejectsDuplicate() {
    when(userRepository.existsByUserName("alice")).thenReturn(true);
    assertThatThrownBy(() -> userService.createUser("alice", "x", UserRole.USER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void createUserRejectsSuperadminRole() {
    when(userRepository.existsByUserName("alice")).thenReturn(false);
    assertThatThrownBy(() -> userService.createUser("alice", "x", UserRole.SUPERADMIN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Superadmin");
  }

  @Test
  void getUserThrowsWhenMissing() {
    when(userRepository.findById(5L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> userService.getUser(5L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateUserRejectsReservedName() {
    assertThatThrownBy(() -> userService.updateUser(1L, SUPERADMIN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("reserved");
  }

  @Test
  void updateUserRejectsNameTakenByAnother() {
    User other = TestFixtures.user(2L, "taken");
    when(userRepository.findByUserName("taken")).thenReturn(Optional.of(other));
    assertThatThrownBy(() -> userService.updateUser(1L, "taken"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void updateUserRenamesWhenNameFree() {
    when(userRepository.findByUserName("fresh")).thenReturn(Optional.empty());
    when(userRepository.findById(1L)).thenReturn(Optional.of(TestFixtures.user(1L, "old")));
    when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

    User updated = userService.updateUser(1L, "fresh");

    assertThat(updated.getUserName()).isEqualTo("fresh");
  }

  @Test
  void deleteUserLoadsThenDeletes() {
    User user = TestFixtures.user(1L, "bob");
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    userService.deleteUser(1L);

    verify(userRepository).delete(user);
  }
}
