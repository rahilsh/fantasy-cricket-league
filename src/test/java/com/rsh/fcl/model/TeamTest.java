package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamTest {

  @Test
  void addPlayerSetsBackReferenceAndStoresPlayer() {
    Team team = new Team("Team Alpha");
    Player player = new Player(1L, "Alice", PlayerType.BATTER);

    team.addPlayer(player);

    assertThat(team.getPlayers()).containsExactly(player);
    assertThat(player.getTeam()).isSameAs(team);
  }

  @Test
  void constructorSetsName() {
    assertThat(new Team("Team Beta").getName()).isEqualTo("Team Beta");
  }
}
