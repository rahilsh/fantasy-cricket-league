package com.rsh.fcl.mapper;

import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;

public final class DtoMapper {

  private DtoMapper() {
  }

  public static GameResponse toGameResponse(Game game) {
    return new GameResponse(game.getId(), game.getTeam1(), game.getTeam2(), game.getStatus(),
        game.getK(), game.getOvers(), game.getBallsBowled(), game.getWickets());
  }

  public static UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getUserName(), user.getRole().name());
  }

  public static UserTeamResponse toUserTeamResponse(UserTeam userTeam) {
    return new UserTeamResponse(userTeam.getId(), userTeam.getGame().getId(),
        userTeam.getUserName(), userTeam.getPoints(), userTeam.getPlayers());
  }

  public static BallEventResponse toBallEventResponse(BallEvent ballEvent) {
    return new BallEventResponse(ballEvent.getId(), ballEvent.getGame().getId(),
        ballEvent.getBatsman(), ballEvent.getBowler(), ballEvent.getScore());
  }

  public static LeaderboardEntry toLeaderboardEntry(UserTeam userTeam) {
    return new LeaderboardEntry(userTeam.getUserName(), userTeam.getPoints());
  }
}
