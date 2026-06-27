package com.rsh.fcl.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.TournamentResponse;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DtoMapperTest {

  @Test
  void toGameResponseDerivesTeamNamesAndCricketers() {
    Game game = TestFixtures.game(9L, 4, 5);

    GameResponse response = DtoMapper.toGameResponse(game);

    assertThat(response.id()).isEqualTo(9L);
    assertThat(response.tournamentId()).isEqualTo(1L);
    assertThat(response.team1()).isEqualTo("Team Alpha");
    assertThat(response.team2()).isEqualTo("Team Beta");
    assertThat(response.team1Id()).isEqualTo(1L);
    assertThat(response.team2Id()).isEqualTo(2L);
    assertThat(response.k()).isEqualTo(4);
    assertThat(response.team1Cricketers()).hasSize(11);
    assertThat(response.team2Cricketers()).hasSize(11);
    assertThat(response.team1Cricketers().get(0).type()).isEqualTo(CricketerType.WICKETKEEPER);
  }

  @Test
  void toTournamentResponseMapsTeams() {
    Game game = TestFixtures.game(1L, 3, 5);

    TournamentResponse response = DtoMapper.toTournamentResponse(game.getTournament());

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("Premier League");
    assertThat(response.teams()).hasSize(2);
    assertThat(response.teams().get(0).cricketers()).hasSize(11);
  }

  @Test
  void toUserResponseMapsRoleName() {
    User user = new User("alice", "hash", User.UserRole.USER);
    user.setId(3L);

    UserResponse response = DtoMapper.toUserResponse(user);

    assertThat(response.id()).isEqualTo(3L);
    assertThat(response.userName()).isEqualTo("alice");
    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void toUserTeamResponseMapsCricketerIds() {
    Game game = TestFixtures.game(1L, 3, 5);
    Set<Cricketer> cricketers = new LinkedHashSet<>();
    cricketers.add(new Cricketer("abc_a1", "P1", CricketerType.WICKETKEEPER));
    cricketers.add(new Cricketer("abc_a2", "P2", CricketerType.BOWLER));
    UserTeam userTeam = new UserTeam(game, TestFixtures.user(2L, "bob"), cricketers);
    userTeam.setId(5L);
    userTeam.setPoints(8.0);

    UserTeamResponse response = DtoMapper.toUserTeamResponse(userTeam);

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.gameId()).isEqualTo(1L);
    assertThat(response.userName()).isEqualTo("bob");
    assertThat(response.points()).isEqualTo(8.0);
    assertThat(response.cricketers()).containsExactly("abc_a1", "abc_a2");
  }

  @Test
  void toBallEventResponseMapsFields() {
    Game game = TestFixtures.game(1L, 3, 5);
    BallEvent event = new BallEvent(game, "abc_a4", "abc_b7", 6);
    event.setId(11L);

    BallEventResponse response = DtoMapper.toBallEventResponse(event);

    assertThat(response.id()).isEqualTo(11L);
    assertThat(response.gameId()).isEqualTo(1L);
    assertThat(response.batsman()).isEqualTo("abc_a4");
    assertThat(response.bowler()).isEqualTo("abc_b7");
    assertThat(response.score()).isEqualTo(6);
  }

  @Test
  void toLeaderboardEntryMapsNameAndPoints() {
    Game game = TestFixtures.game(1L, 3, 5);
    UserTeam userTeam = new UserTeam(game, TestFixtures.user(2L, "carol"), Set.of());
    userTeam.setPoints(15.5);

    LeaderboardEntry entry = DtoMapper.toLeaderboardEntry(userTeam);

    assertThat(entry.userName()).isEqualTo("carol");
    assertThat(entry.points()).isEqualTo(15.5);
  }
}
