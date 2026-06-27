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

  /** Eleven valid players (1 WK, 4 bowlers, 3 all-rounders, 3 batters). */
  public static List<PlayerRequest> playerRequests(long baseId, String prefix) {
    List<PlayerRequest> players = new ArrayList<>();
    for (int offset = 0; offset < 11; offset++) {
      players.add(new PlayerRequest(baseId + offset, prefix + " P" + (offset + 1),
          typeForOffset(offset)));
    }
    return players;
  }

  public static Team team(String name, long baseId) {
    Team team = new Team(name);
    for (int offset = 0; offset < 11; offset++) {
      team.addPlayer(new Player(baseId + offset, name + " P" + (offset + 1),
          typeForOffset(offset)));
    }
    return team;
  }

  /** A persisted-looking game with two valid 11-player squads (ids 1..11 and 12..22). */
  public static Game game(Long id, int k, int overs) {
    Game game = new Game(k, overs);
    game.setId(id);
    game.addTeam(team("Team Alpha", 1));
    game.addTeam(team("Team Beta", 12));
    return game;
  }

  public static User user(Long id, String userName) {
    User user = new User(userName);
    user.setId(id);
    return user;
  }
}
