package com.rsh.fcl.mapper;

import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.dto.CricketerResponse;
import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.TeamResponse;
import com.rsh.fcl.dto.TournamentResponse;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DtoMapper {

  private DtoMapper() {
  }

  public static CricketerResponse toCricketerResponse(Cricketer cricketer) {
    return new CricketerResponse(cricketer.getGlobalUniqueId(), cricketer.getName(),
        cricketer.getType());
  }

  public static TeamResponse toTeamResponse(Team team) {
    return new TeamResponse(team.getId(), team.getName(), team.getTournament().getId(),
        toCricketers(team.getCricketers()));
  }

  public static TournamentResponse toTournamentResponse(Tournament tournament) {
    return new TournamentResponse(tournament.getId(), tournament.getName(), tournament.getStatus(),
        tournament.getTeams().stream().map(DtoMapper::toTeamResponse).toList());
  }

  public static GameResponse toGameResponse(Game game) {
    Team team1 = game.getTeam1();
    Team team2 = game.getTeam2();
    return new GameResponse(game.getId(), game.getTournament().getId(), team1.getId(), team2.getId(),
        team1.getName(), team2.getName(), game.getStatus(), game.getK(), game.getOvers(),
        game.getBallsBowled(), game.getWickets(),
        toCricketers(team1.getCricketers()), toCricketers(team2.getCricketers()));
  }

  public static UserResponse toUserResponse(User user) {
    return new UserResponse(user.getId(), user.getUserName(), user.getRole().name());
  }

  public static UserTeamResponse toUserTeamResponse(UserTeam userTeam) {
    return new UserTeamResponse(userTeam.getId(), userTeam.getGame().getId(),
        userTeam.getUserName(), userTeam.getPoints(), toCricketerIds(userTeam.getCricketers()));
  }

  public static BallEventResponse toBallEventResponse(BallEvent ballEvent) {
    return new BallEventResponse(ballEvent.getId(), ballEvent.getGame().getId(),
        ballEvent.getBatsman(), ballEvent.getBowler(), ballEvent.getScore());
  }

  public static LeaderboardEntry toLeaderboardEntry(UserTeam userTeam) {
    return new LeaderboardEntry(userTeam.getUserName(), userTeam.getPoints());
  }

  private static List<CricketerResponse> toCricketers(Collection<Cricketer> cricketers) {
    return cricketers.stream()
        .map(DtoMapper::toCricketerResponse)
        .toList();
  }

  private static Set<String> toCricketerIds(Collection<Cricketer> cricketers) {
    return cricketers.stream()
        .map(Cricketer::getGlobalUniqueId)
        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
  }
}
