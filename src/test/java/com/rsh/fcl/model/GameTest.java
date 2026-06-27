package com.rsh.fcl.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.support.TestFixtures;
import org.junit.jupiter.api.Test;

class GameTest {

  @Test
  void totalBallsIsSixPerOver() {
    assertThat(new Game(3, 5).totalBalls()).isEqualTo(30);
  }

  @Test
  void addTeamLinksGameAndCollectsAllPlayers() {
    Game game = new Game(3, 5);
    game.addTeam(TestFixtures.team("Team Alpha", "a"));
    game.addTeam(TestFixtures.team("Team Beta", "b"));

    assertThat(game.getTeams()).hasSize(2);
    assertThat(game.getTeams()).allSatisfy(team -> assertThat(team.getGame()).isSameAs(game));
    assertThat(game.getAllPlayers()).hasSize(22);
  }

  @Test
  void inningsIsOverWhenAllBallsBowled() {
    Game game = new Game(3, 1);
    game.setBallsBowled(6);
    assertThat(game.isInningsOver()).isTrue();
  }

  @Test
  void inningsIsOverWhenAllOut() {
    Game game = new Game(3, 20);
    game.setWickets(Game.ALL_OUT_WICKETS);
    assertThat(game.isInningsOver()).isTrue();
  }

  @Test
  void inningsIsNotOverMidGame() {
    Game game = new Game(3, 20);
    game.setBallsBowled(5);
    game.setWickets(3);
    assertThat(game.isInningsOver()).isFalse();
  }

  @Test
  void newGameStartsInCreatedState() {
    assertThat(new Game(3, 5).getStatus()).isEqualTo(Game.GameStatus.CREATED);
  }
}
