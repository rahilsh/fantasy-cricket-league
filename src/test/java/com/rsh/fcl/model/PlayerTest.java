package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlayerTest {

  @Test
  void playersAreEqualWhenGlobalUniqueIdMatches() {
    Player a = new Player(7L, "Alice", PlayerType.BATTER);
    Player b = new Player(7L, "Different Name", PlayerType.BOWLER);

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
  }

  @Test
  void playersDifferWhenGlobalUniqueIdDiffers() {
    Player a = new Player(7L, "Alice", PlayerType.BATTER);
    Player b = new Player(8L, "Alice", PlayerType.BATTER);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void playerIsNotEqualToOtherTypesOrNull() {
    Player a = new Player(7L, "Alice", PlayerType.BATTER);

    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("not-a-player");
    assertThat(a).isEqualTo(a);
  }

  @Test
  void constructorPopulatesFields() {
    Player player = new Player(42L, "Zed", PlayerType.WICKETKEEPER);

    assertThat(player.getGlobalUniqueId()).isEqualTo(42L);
    assertThat(player.getName()).isEqualTo("Zed");
    assertThat(player.getType()).isEqualTo(PlayerType.WICKETKEEPER);
  }
}
