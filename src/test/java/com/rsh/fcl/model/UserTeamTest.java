package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTeamTest {

  private UserTeam userTeam(Long gameId, Long userId, String... playerIds) {
    Game game = new Game(3, 5);
    game.setId(gameId);
    Set<Player> players = new LinkedHashSet<>();
    for (String id : playerIds) {
      players.add(new Player(id, "P-" + id, PlayerType.BATTER));
    }
    return new UserTeam(game, TestFixtures.user(userId, "user" + userId), players);
  }

  @Test
  void hasPlayerReflectsMembership() {
    UserTeam team = userTeam(1L, 1L, "a5", "a6", "a7");

    assertThat(team.hasPlayer("a6")).isTrue();
    assertThat(team.hasPlayer("zz9")).isFalse();
  }

  @Test
  void getUserNameReadsThroughToUser() {
    assertThat(userTeam(1L, 2L, "a5").getUserName()).isEqualTo("user2");
  }

  @Test
  void getUserNameIsNullWhenNoUser() {
    UserTeam team = new UserTeam();
    assertThat(team.getUserName()).isNull();
  }

  @Test
  void equalityIsByGameAndUser() {
    UserTeam a = userTeam(1L, 1L, "a5");
    UserTeam b = userTeam(1L, 1L, "a9");
    UserTeam differentUser = userTeam(1L, 2L, "a5");

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(differentUser);
    assertThat(a).isNotEqualTo(null);
  }
}
