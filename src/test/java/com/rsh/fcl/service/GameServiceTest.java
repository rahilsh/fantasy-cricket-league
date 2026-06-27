package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.BallEventNotSupportedException;
import com.rsh.fcl.exception.GameAlreadyCompletedException;
import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.GameNotStartedException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.repository.TournamentRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

  @Mock
  private GameRepository gameRepository;
  @Mock
  private TournamentRepository tournamentRepository;
  @Mock
  private TeamRepository teamRepository;
  @Mock
  private UserTeamRepository userTeamRepository;
  @Mock
  private BallEventRepository ballEventRepository;

  private GameService gameService;

  private Tournament tournament;
  private Team alpha;
  private Team beta;

  @BeforeEach
  void setUp() {
    gameService = new GameService(gameRepository, tournamentRepository, teamRepository,
        userTeamRepository, ballEventRepository);
    lenient().when(gameRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    tournament = TestFixtures.tournament(1L, "Premier League");
    tournament.setStatus(Tournament.TournamentStatus.IN_PROGRESS);
    alpha = TestFixtures.team("Team Alpha", "a");
    alpha.setId(1L);
    beta = TestFixtures.team("Team Beta", "b");
    beta.setId(2L);
    tournament.addTeam(alpha);
    tournament.addTeam(beta);
  }

  private void stubTournamentAndTeams() {
    lenient().when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    lenient().when(teamRepository.findById(1L)).thenReturn(Optional.of(alpha));
    lenient().when(teamRepository.findById(2L)).thenReturn(Optional.of(beta));
  }

  @Test
  void createGameLinksTournamentTeamsAndRoster() {
    stubTournamentAndTeams();

    Game game = gameService.createGame(1L, 1L, 2L, 3, 5);

    assertThat(game.getTournament()).isSameAs(tournament);
    assertThat(game.getTeam1()).isSameAs(alpha);
    assertThat(game.getTeam2()).isSameAs(beta);
    assertThat(game.getAllCricketers()).hasSize(22);
    verify(gameRepository).save(game);
  }

  @Test
  void createGameRejectsNonPositiveTopK() {
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 2L, 0, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Top K");
  }

  @Test
  void createGameRejectsNonPositiveOvers() {
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 2L, 3, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Overs");
  }

  @Test
  void createGameRejectsMissingTournament() {
    when(tournamentRepository.findById(9L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> gameService.createGame(9L, 1L, 2L, 3, 5))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Tournament");
  }

  @Test
  void createGameRejectsCompletedTournament() {
    tournament.setStatus(Tournament.TournamentStatus.COMPLETED);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 2L, 3, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("completed tournament");
  }

  @Test
  void createGameRejectsTeamFromAnotherTournament() {
    Tournament other = TestFixtures.tournament(2L, "Other");
    Team foreign = TestFixtures.team("Foreign", "c");
    foreign.setId(3L);
    other.addTeam(foreign);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(alpha));
    when(teamRepository.findById(3L)).thenReturn(Optional.of(foreign));

    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 3L, 3, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not belong to tournament");
  }

  @Test
  void createGameRejectsSameTeamTwice() {
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(teamRepository.findById(1L)).thenReturn(Optional.of(alpha));
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 1L, 3, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("two distinct teams");
  }

  @Test
  void createGameRejectsTeamWithoutElevenCricketers() {
    alpha.getCricketers().remove(alpha.getCricketers().iterator().next());
    stubTournamentAndTeams();
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 2L, 3, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly 11 cricketers");
  }

  @Test
  void createGameRejectsTeamWithoutWicketkeeper() {
    alpha.setCricketers(new LinkedHashSet<>());
    for (int offset = 0; offset < 11; offset++) {
      CricketerType type = offset <= 4 ? CricketerType.BOWLER : CricketerType.BATTER;
      alpha.addCricketer(TestFixtures.cricketer("abc_a" + (offset + 1), type));
    }
    stubTournamentAndTeams();
    assertThatThrownBy(() -> gameService.createGame(1L, 1L, 2L, 3, 5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one wicketkeeper");
  }

  @Test
  void updateGameReplacesTeamsAndSettings() {
    Game existing = TestFixtures.game(5L, 3, 5);
    when(gameRepository.findById(5L)).thenReturn(Optional.of(existing));
    stubTournamentAndTeams();

    Game updated = gameService.updateGame(5L, 1L, 1L, 2L, 4, 10);

    assertThat(updated.getK()).isEqualTo(4);
    assertThat(updated.getOvers()).isEqualTo(10);
    assertThat(updated.getTeam1()).isSameAs(alpha);
    assertThat(updated.getTeam2()).isSameAs(beta);
  }

  @Test
  void updateGameThrowsWhenGameMissing() {
    when(gameRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> gameService.updateGame(99L, 1L, 1L, 2L, 3, 5))
        .isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void getGameThrowsWhenMissing() {
    when(gameRepository.findById(7L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> gameService.getGame(7L)).isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void startGameMovesCreatedToInProgress() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThat(gameService.startGame(1L).getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
  }

  @Test
  void startGameRejectsCompletedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.COMPLETED);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.startGame(1L))
        .isInstanceOf(GameAlreadyCompletedException.class);
  }

  @Test
  void startGameRejectsWhenTeamAlreadyInLiveGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    Game otherLive = TestFixtures.game(2L, 3, 5);
    otherLive.setStatus(GameStatus.IN_PROGRESS);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(gameRepository.findByStatusAndTeam(eq(GameStatus.IN_PROGRESS), any()))
        .thenReturn(List.of(otherLive));

    assertThatThrownBy(() -> gameService.startGame(1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already in another in-progress game");
  }

  @Test
  void endGameRejectsCreatedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.endGame(1L)).isInstanceOf(GameNotStartedException.class);
  }

  @Test
  void endGameCompletesInProgressGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.IN_PROGRESS);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThat(gameService.endGame(1L).getStatus()).isEqualTo(GameStatus.COMPLETED);
  }

  @Test
  void deleteGameDelegatesToRepository() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    gameService.deleteGame(1L);

    verify(gameRepository).delete(game);
  }

  private UserTeam userTeamWith(String... cricketerIds) {
    Set<Cricketer> cricketers = new LinkedHashSet<>();
    for (String id : cricketerIds) {
      cricketers.add(new Cricketer(id, "C-" + id, CricketerType.BATTER));
    }
    return new UserTeam(TestFixtures.game(1L, 3, 5), TestFixtures.user(1L, "bob"), cricketers);
  }

  private Game inProgressGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.IN_PROGRESS);
    return game;
  }

  @Test
  void playRejectsGameNotStarted() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> gameService.play(1L, "abc_a1", "abc_b1", 4))
        .isInstanceOf(GameNotStartedException.class);
  }

  @Test
  void playRejectsCompletedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.COMPLETED);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> gameService.play(1L, "abc_a1", "abc_b1", 4))
        .isInstanceOf(GameAlreadyCompletedException.class);
  }

  @Test
  void playRejectsUnsupportedOutcome() {
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith("abc_a1")));
    assertThatThrownBy(() -> gameService.play(1L, "abc_a1", "abc_b1", 3))
        .isInstanceOf(BallEventNotSupportedException.class);
  }

  @Test
  void playAwardsRunsToBatsmanOwners() {
    UserTeam batsmanOwner = userTeamWith("abc_a1");
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(batsmanOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "abc_a1", "abc_b1", 6);

    assertThat(batsmanOwner.getPoints()).isEqualTo(3.0);
  }

  @Test
  void playPenalisesBowlerAndCountsWicket() {
    UserTeam bowlerOwner = userTeamWith("abc_b1");
    Game game = inProgressGame();
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(bowlerOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "abc_a1", "abc_b1", -1);

    assertThat(bowlerOwner.getPoints()).isEqualTo(4.0);
    assertThat(game.getWickets()).isEqualTo(1);
    assertThat(game.getBallsBowled()).isEqualTo(1);
  }

  @Test
  void playCompletesGameWhenInningsOver() {
    Game game = inProgressGame();
    game.setWickets(Game.ALL_OUT_WICKETS - 1);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith("abc_b1")));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "abc_a1", "abc_b1", -1);

    assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
  }

  @Test
  void leaderboardReturnsTopKSortedByPoints() {
    Game game = TestFixtures.game(1L, 2, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    when(userTeamRepository.findByGameId(1L)).thenReturn(List.of(
        teamWithPoints("low", 1.0), teamWithPoints("high", 9.0), teamWithPoints("mid", 5.0)));

    List<UserTeam> leaderboard = gameService.getLeaderboard(1L);

    assertThat(leaderboard).extracting(UserTeam::getUserName).containsExactly("high", "mid");
  }

  private UserTeam teamWithPoints(String name, double points) {
    UserTeam team = new UserTeam(TestFixtures.game(1L, 2, 5), TestFixtures.user(1L, name), Set.of());
    team.setPoints(points);
    return team;
  }

  @Test
  void playSavesUpdatedUserTeams() {
    UserTeam owner = userTeamWith("abc_a1");
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(owner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "abc_a1", "abc_b1", 1);

    ArgumentCaptor<List<UserTeam>> captor = ArgumentCaptor.forClass(List.class);
    verify(userTeamRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).containsExactly(owner);
    verify(ballEventRepository).save(any(BallEvent.class));
    verify(userTeamRepository, never()).deleteAll();
  }
}
