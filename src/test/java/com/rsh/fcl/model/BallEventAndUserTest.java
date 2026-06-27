package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;

import org.junit.jupiter.api.Test;

class BallEventAndUserTest {

  @Test
  void ballEventConstructorPopulatesFields() {
    Game game = TestFixtures.game(1L, 3, 5);
    BallEvent event = new BallEvent(game, "abc_a4", "abc_b7", 6);

    assertThat(event.getGame()).isSameAs(game);
    assertThat(event.getBatsman()).isEqualTo("abc_a4");
    assertThat(event.getBowler()).isEqualTo("abc_b7");
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
