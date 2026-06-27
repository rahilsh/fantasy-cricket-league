package com.rsh.fcl.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequestDefaultsTest {

  @Test
  void topKDefaultsToThreeWhenNull() {
    GameRequest request = new GameRequest(1L, 1L, 2L, null, 5);
    assertThat(request.topKOrDefault()).isEqualTo(3);
  }

  @Test
  void topKUsesProvidedValue() {
    GameRequest request = new GameRequest(1L, 1L, 2L, 7, 5);
    assertThat(request.topKOrDefault()).isEqualTo(7);
  }

  @Test
  void pointsDefaultToZeroWhenNull() {
    UserTeamRequest request = new UserTeamRequest(1L, "u", List.of(), null);
    assertThat(request.pointsOrDefault()).isZero();
  }

  @Test
  void pointsUseProvidedValue() {
    UserTeamRequest request = new UserTeamRequest(1L, "u", List.of(), 12.5);
    assertThat(request.pointsOrDefault()).isEqualTo(12.5);
  }
}
