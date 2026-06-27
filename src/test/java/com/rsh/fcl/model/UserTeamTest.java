package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserTeamTest {

  private UserTeam userTeam(Long gameId, Long userId, long... playerIds) {
    Game game = new Game(3, 5);
    game.setId(gameId);
    Set<Player> players = new LinkedHashSet<>();
    for (long id : playerIds) {
      players.add(new Player(id, "P" + id, PlayerType.BATTER));
    }
    return new UserTeam(game, TestFixtures.user(userId, "user" + userId), players);
  }

  @Test
  void hasPlayerReflectsMembership() {
    UserTeam team = userTeam(1L, 1L, 5L, 6L, 7L);

    assertThat(team.hasPlayer(6L)).isTrue();
    assertThat(team.hasPlayer(99L)).isFalse();
  }

  @Test
  void getUserNameReadsThroughToUser() {
    assertThat(userTeam(1L, 2L, 5L).getUserName()).isEqualTo("user2");
  }

  @Test
  void getUserNameIsNullWhenNoUser() {
    UserTeam team = new UserTeam();
    assertThat(team.getUserName()).isNull();
  }

  @Test
  void equalityIsByGameAndUser() {
    UserTeam a = userTeam(1L, 1L, 5L);
    UserTeam b = userTeam(1L, 1L, 9L);
    UserTeam differentUser = userTeam(1L, 2L, 5L);

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
    assertThat(a).isNotEqualTo(differentUser);
    assertThat(a).isNotEqualTo(null);
  }
}
