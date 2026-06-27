package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamTest {

  @Test
  void addCricketerStoresCricketer() {
    Team team = new Team("Team Alpha");
    Cricketer cricketer = new Cricketer("abc_xyz", "Alice", CricketerType.BATTER);

    team.addCricketer(cricketer);

    assertThat(team.getCricketers()).containsExactly(cricketer);
    assertThat(team.hasCricketer("abc_xyz")).isTrue();
    assertThat(team.hasCricketer("zzz_zzz")).isFalse();
  }

  @Test
  void constructorSetsName() {
    assertThat(new Team("Team Beta").getName()).isEqualTo("Team Beta");
  }
}
