package com.rsh.fcl.support;

import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.User;
import java.util.ArrayList;
import java.util.List;

/** Shared builders for unit tests so each test stays focused on behaviour. */
public final class TestFixtures {

  private TestFixtures() {
  }

  public static CricketerType typeForOffset(int offset) {
    if (offset == 0) {
      return CricketerType.WICKETKEEPER;
    }
    if (offset <= 4) {
      return CricketerType.BOWLER;
    }
    if (offset <= 7) {
      return CricketerType.ALLROUNDER;
    }
    return CricketerType.BATTER;
  }

  public static Cricketer cricketer(String id, CricketerType type) {
    return new Cricketer(id, "C-" + id, type);
  }

  /** Cricketer id used by fixtures, e.g. {@code "abc_a1"} for offset 0 of prefix {@code "a"}. */
  public static String cricketerId(String prefix, int offset) {
    return "abc_" + prefix + (offset + 1);
  }

  /** Eleven valid cricketer ids (1 WK, 4 bowlers, 3 all-rounders, 3 batters). */
  public static List<String> cricketerIds(String prefix) {
    List<String> ids = new ArrayList<>();
    for (int offset = 0; offset < 11; offset++) {
      ids.add(cricketerId(prefix, offset));
    }
    return ids;
  }

  public static Team team(String name, String idPrefix) {
    Team team = new Team(name);
    for (int offset = 0; offset < 11; offset++) {
      team.addCricketer(cricketer(cricketerId(idPrefix, offset), typeForOffset(offset)));
    }
    return team;
  }

  public static Tournament tournament(Long id, String name) {
    Tournament tournament = new Tournament(name);
    tournament.setId(id);
    return tournament;
  }

  /**
   * A persisted-looking game with two valid 11-cricketer squads. Team Alpha ids are
   * {@code abc_a1..abc_a11} and Team Beta ids are {@code abc_b1..abc_b11} (offset 0 = WICKETKEEPER,
   * 1-4 = BOWLER, 5-7 = ALLROUNDER, 8-10 = BATTER).
   */
  public static Game game(Long id, int k, int overs) {
    Tournament tournament = tournament(1L, "Premier League");
    Team alpha = team("Team Alpha", "a");
    alpha.setId(1L);
    Team beta = team("Team Beta", "b");
    beta.setId(2L);
    tournament.addTeam(alpha);
    tournament.addTeam(beta);
    Game game = new Game(tournament, alpha, beta, k, overs);
    game.setId(id);
    return game;
  }

  public static User user(Long id, String userName) {
    User user = new User(userName);
    user.setId(id);
    return user;
  }
}
