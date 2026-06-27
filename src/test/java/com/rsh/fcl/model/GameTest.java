package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;
import org.junit.jupiter.api.Test;

class GameTest {

  @Test
  void totalBallsIsSixPerOver() {
    assertThat(TestFixtures.game(1L, 3, 5).totalBalls()).isEqualTo(30);
  }

  @Test
  void gameExposesAllCricketersFromBothTeams() {
    Game game = TestFixtures.game(1L, 3, 5);

    assertThat(game.getTeam1().getName()).isEqualTo("Team Alpha");
    assertThat(game.getTeam2().getName()).isEqualTo("Team Beta");
    assertThat(game.getAllCricketers()).hasSize(22);
  }

  @Test
  void inningsIsOverWhenAllBallsBowled() {
    Game game = TestFixtures.game(1L, 3, 1);
    game.setBallsBowled(6);
    assertThat(game.isInningsOver()).isTrue();
  }

  @Test
  void inningsIsOverWhenAllOut() {
    Game game = TestFixtures.game(1L, 3, 20);
    game.setWickets(Game.ALL_OUT_WICKETS);
    assertThat(game.isInningsOver()).isTrue();
  }

  @Test
  void inningsIsNotOverMidGame() {
    Game game = TestFixtures.game(1L, 3, 20);
    game.setBallsBowled(5);
    game.setWickets(3);
    assertThat(game.isInningsOver()).isFalse();
  }

  @Test
  void newGameStartsInCreatedState() {
    assertThat(TestFixtures.game(1L, 3, 5).getStatus()).isEqualTo(Game.GameStatus.CREATED);
  }
}
