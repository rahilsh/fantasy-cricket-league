package com.rsh.fcl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  public enum UserRole {
    USER,
    ADMIN,
    SUPERADMIN
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_name", nullable = false, unique = true)
  private String userName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role = UserRole.USER;

  public User(String userName) {
    this.userName = userName;
    this.passwordHash = "";
    this.role = UserRole.USER;
  }

  public User(String userName, String passwordHash, UserRole role) {
    this.userName = userName;
    this.passwordHash = passwordHash;
    this.role = role;
  }
}
