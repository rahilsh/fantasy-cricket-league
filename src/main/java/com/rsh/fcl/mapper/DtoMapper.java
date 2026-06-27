package com.rsh.fcl.mapper;

import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.dto.PlayerResponse;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DtoMapper {

  private DtoMapper() {
  }

  public static GameResponse toGameResponse(Game game) {
    List<Team> teams = new ArrayList<>(game.getTeams());
    Team team1 = teams.get(0);
    Team team2 = teams.get(1);
    return new GameResponse(game.getId(), team1.getName(), team2.getName(), game.getStatus(),
        game.getK(), game.getOvers(), game.getBallsBowled(), game.getWickets(),
        toPlayers(team1.getPlayers()),
        toPlayers(team2.getPlayers()));
  }

  public static UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getUserName(), user.getRole().name());
  }

  public static UserTeamResponse toUserTeamResponse(UserTeam userTeam) {
    return new UserTeamResponse(userTeam.getId(), userTeam.getGame().getId(),
        userTeam.getUserName(), userTeam.getPoints(), toPlayerIds(userTeam.getPlayers()));
  }

  public static BallEventResponse toBallEventResponse(BallEvent ballEvent) {
    return new BallEventResponse(ballEvent.getId(), ballEvent.getGame().getId(),
        ballEvent.getBatsman(), ballEvent.getBowler(), ballEvent.getScore());
  }

  public static LeaderboardEntry toLeaderboardEntry(UserTeam userTeam) {
    return new LeaderboardEntry(userTeam.getUserName(), userTeam.getPoints());
  }

  private static List<PlayerResponse> toPlayers(Set<Player> players) {
    return players.stream()
        .map(player -> new PlayerResponse(player.getGlobalUniqueId(), player.getName(),
            player.getType()))
        .toList();
  }

  private static Set<String> toPlayerIds(Set<Player> players) {
    return players.stream()
        .map(Player::getGlobalUniqueId)
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
  }
}
