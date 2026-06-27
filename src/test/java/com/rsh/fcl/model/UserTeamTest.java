package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTeamTest {

  private UserTeam userTeam(Long gameId, Long userId, String... cricketerIds) {
    Game game = TestFixtures.game(gameId, 3, 5);
    Set<Cricketer> cricketers = new LinkedHashSet<>();
    for (String id : cricketerIds) {
      cricketers.add(new Cricketer(id, "C-" + id, CricketerType.BATTER));
    }
    return new UserTeam(game, TestFixtures.user(userId, "user" + userId), cricketers);
  }

  @Test
  void hasPlayerReflectsMembership() {
    UserTeam team = userTeam(1L, 1L, "abc_a5", "abc_a6", "abc_a7");

    assertThat(team.hasPlayer("abc_a6")).isTrue();
    assertThat(team.hasPlayer("zzz_zz9")).isFalse();
  }

  @Test
  void getUserNameReadsThroughToUser() {
    assertThat(userTeam(1L, 2L, "abc_a5").getUserName()).isEqualTo("user2");
  }

  @Test
  void getUserNameIsNullWhenNoUser() {
    UserTeam team = new UserTeam();
    assertThat(team.getUserName()).isNull();
  }

  @Test
  void equalityIsByGameAndUser() {
    UserTeam a = userTeam(1L, 1L, "abc_a5");
    UserTeam b = userTeam(1L, 1L, "abc_a9");
    UserTeam differentUser = userTeam(1L, 2L, "abc_a5");

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(differentUser);
    assertThat(a).isNotEqualTo(null);
  }
}
