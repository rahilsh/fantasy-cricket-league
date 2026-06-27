package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlayerTest {

  @Test
  void playersAreEqualWhenGlobalUniqueIdMatches() {
    Player a = new Player("brave_lion", "Alice", PlayerType.BATTER);
    Player b = new Player("brave_lion", "Different Name", PlayerType.BOWLER);

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
  }

  @Test
  void playersDifferWhenGlobalUniqueIdDiffers() {
    Player a = new Player("brave_lion", "Alice", PlayerType.BATTER);
    Player b = new Player("calm_otter", "Alice", PlayerType.BATTER);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void playerIsNotEqualToOtherTypesOrNull() {
    Player a = new Player("brave_lion", "Alice", PlayerType.BATTER);

    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("not-a-player");
    assertThat(a).isEqualTo(a);
  }

  @Test
  void constructorPopulatesFields() {
    Player player = new Player("swift_eagle", "Zed", PlayerType.WICKETKEEPER);

    assertThat(player.getGlobalUniqueId()).isEqualTo("swift_eagle");
    assertThat(player.getName()).isEqualTo("Zed");
    assertThat(player.getType()).isEqualTo(PlayerType.WICKETKEEPER);
  }
}
