package com.rsh.fcl.support;

import com.rsh.fcl.dto.PlayerRequest;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.PlayerType;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.User;
import java.util.ArrayList;
import java.util.List;

/** Shared builders for unit tests so each test stays focused on behaviour. */
public final class TestFixtures {

  private TestFixtures() {
  }

  public static PlayerType typeForOffset(int offset) {
    if (offset == 0) {
      return PlayerType.WICKETKEEPER;
    }
    if (offset <= 4) {
      return PlayerType.BOWLER;
    }
    if (offset <= 7) {
      return PlayerType.ALLROUNDER;
    }
    return PlayerType.BATTER;
  }

  /** Eleven valid player requests (1 WK, 4 bowlers, 3 all-rounders, 3 batters). */
  public static List<PlayerRequest> playerRequests(String prefix) {
    List<PlayerRequest> players = new ArrayList<>();
    for (int offset = 0; offset < 11; offset++) {
      players.add(new PlayerRequest(prefix + " P" + (offset + 1), typeForOffset(offset)));
    }
    return players;
  }

  /** Readable player id used by fixtures, e.g. {@code "a1"} for offset 0 of prefix {@code "a"}. */
  public static String playerId(String prefix, int offset) {
    return prefix + (offset + 1);
  }

  public static Team team(String name, String idPrefix) {
    Team team = new Team(name);
    for (int offset = 0; offset < 11; offset++) {
      team.addPlayer(new Player(playerId(idPrefix, offset), name + " P" + (offset + 1),
          typeForOffset(offset)));
    }
    return team;
  }

  /**
   * A persisted-looking game with two valid 11-player squads. Team Alpha ids are {@code a1..a11}
   * and Team Beta ids are {@code b1..b11} (offset 0 = WICKETKEEPER, 1-4 = BOWLER, 5-7 = ALLROUNDER,
   * 8-10 = BATTER).
   */
  public static Game game(Long id, int k, int overs) {
    Game game = new Game(k, overs);
    game.setId(id);
    game.addTeam(team("Team Alpha", "a"));
    game.addTeam(team("Team Beta", "b"));
    return game;
  }

  public static User user(Long id, String userName) {
    User user = new User(userName);
    user.setId(id);
    return user;
  }
}
