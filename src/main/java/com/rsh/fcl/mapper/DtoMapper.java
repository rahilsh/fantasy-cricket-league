package com.rsh.fcl.mapper;

import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.OutcomeResponse;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Outcome;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;

public final class DtoMapper {

  private DtoMapper() {
  }

  public static GameResponse toGameResponse(Game game) {
    return new GameResponse(game.getId(), game.getTeam1(), game.getTeam2(), game.getStatus(),
        game.getK());
  }

  public static UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getUserName());
  }

  public static UserTeamResponse toUserTeamResponse(UserTeam userTeam) {
    return new UserTeamResponse(userTeam.getId(), userTeam.getGame().getId(),
        userTeam.getUserName(), userTeam.getPoints(), userTeam.getPlayers());
  }

  public static OutcomeResponse toOutcomeResponse(Outcome outcome) {
    return new OutcomeResponse(outcome.getId(), outcome.getGame().getId(),
        outcome.getBatsman(), outcome.getBowler(), outcome.getScore());
  }

  public static LeaderboardEntry toLeaderboardEntry(UserTeam userTeam) {
    return new LeaderboardEntry(userTeam.getUserName(), userTeam.getPoints());
  }
}
