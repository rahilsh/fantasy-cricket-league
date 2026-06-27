package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BallEventAndUserTest {

  @Test
  void ballEventConstructorPopulatesFields() {
    Game game = new Game(3, 5);
    BallEvent event = new BallEvent(game, "a4", "b7", 6);

    assertThat(event.getGame()).isSameAs(game);
    assertThat(event.getBatsman()).isEqualTo("a4");
    assertThat(event.getBowler()).isEqualTo("b7");
    assertThat(event.getScore()).isEqualTo(6);
  }

  @Test
  void userNameOnlyConstructorDefaultsRoleAndEmptyHash() {
    User user = new User("alice");

    assertThat(user.getUserName()).isEqualTo("alice");
    assertThat(user.getPasswordHash()).isEmpty();
    assertThat(user.getRole()).isEqualTo(User.UserRole.USER);
  }

  @Test
  void fullUserConstructorPopulatesFields() {
    User user = new User("admin", "hash", User.UserRole.SUPERADMIN);

    assertThat(user.getUserName()).isEqualTo("admin");
    assertThat(user.getPasswordHash()).isEqualTo("hash");
    assertThat(user.getRole()).isEqualTo(User.UserRole.SUPERADMIN);
  }
}
