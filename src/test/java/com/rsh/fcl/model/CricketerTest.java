package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CricketerTest {

  @Test
  void cricketersAreEqualWhenGlobalUniqueIdMatches() {
    Cricketer a = new Cricketer("abc_lio", "Alice", CricketerType.BATTER);
    Cricketer b = new Cricketer("abc_lio", "Different Name", CricketerType.BOWLER);

    assertThat(a).isEqualTo(b);
    assertThat(a).hasSameHashCodeAs(b);
  }

  @Test
  void cricketersDifferWhenGlobalUniqueIdDiffers() {
    Cricketer a = new Cricketer("abc_lio", "Alice", CricketerType.BATTER);
    Cricketer b = new Cricketer("cot_ter", "Alice", CricketerType.BATTER);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void cricketerIsNotEqualToOtherTypesOrNull() {
    Cricketer a = new Cricketer("abc_lio", "Alice", CricketerType.BATTER);

    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("not-a-cricketer");
    assertThat(a).isEqualTo(a);
  }

  @Test
  void constructorPopulatesFields() {
    Cricketer cricketer = new Cricketer("swi_egl", "Zed", CricketerType.WICKETKEEPER);

    assertThat(cricketer.getGlobalUniqueId()).isEqualTo("swi_egl");
    assertThat(cricketer.getName()).isEqualTo("Zed");
    assertThat(cricketer.getType()).isEqualTo(CricketerType.WICKETKEEPER);
  }
}
