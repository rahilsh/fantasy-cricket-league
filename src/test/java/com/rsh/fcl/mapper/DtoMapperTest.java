package com.rsh.fcl.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rsh.fcl.dto.BallEventResponse;
import com.rsh.fcl.dto.GameResponse;
import com.rsh.fcl.dto.LeaderboardEntry;
import com.rsh.fcl.dto.UserResponse;
import com.rsh.fcl.dto.UserTeamResponse;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.PlayerType;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DtoMapperTest {

  @Test
  void toGameResponseDerivesTeamNamesAndPlayers() {
    Game game = TestFixtures.game(9L, 4, 5);

    GameResponse response = DtoMapper.toGameResponse(game);

    assertThat(response.id()).isEqualTo(9L);
    assertThat(response.team1()).isEqualTo("Team Alpha");
    assertThat(response.team2()).isEqualTo("Team Beta");
    assertThat(response.k()).isEqualTo(4);
    assertThat(response.team1Players()).hasSize(11);
    assertThat(response.team2Players()).hasSize(11);
    assertThat(response.team1Players().get(0).type()).isEqualTo(PlayerType.WICKETKEEPER);
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
  void toUserTeamResponseMapsPlayerIds() {
    Game game = TestFixtures.game(1L, 3, 5);
    Set<Player> players = new LinkedHashSet<>();
    players.add(new Player("a1", "P1", PlayerType.WICKETKEEPER));
    players.add(new Player("a2", "P2", PlayerType.BOWLER));
    UserTeam userTeam = new UserTeam(game, TestFixtures.user(2L, "bob"), players);
    userTeam.setId(5L);
    userTeam.setPoints(8.0);

    UserTeamResponse response = DtoMapper.toUserTeamResponse(userTeam);

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.gameId()).isEqualTo(1L);
    assertThat(response.userName()).isEqualTo("bob");
    assertThat(response.points()).isEqualTo(8.0);
    assertThat(response.players()).containsExactly("a1", "a2");
  }

  @Test
  void toBallEventResponseMapsFields() {
    Game game = TestFixtures.game(1L, 3, 5);
    BallEvent event = new BallEvent(game, "a4", "b7", 6);
    event.setId(11L);

    BallEventResponse response = DtoMapper.toBallEventResponse(event);

    assertThat(response.id()).isEqualTo(11L);
    assertThat(response.gameId()).isEqualTo(1L);
    assertThat(response.batsman()).isEqualTo("a4");
    assertThat(response.bowler()).isEqualTo("b7");
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
