package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.dto.PlayerRequest;
import com.rsh.fcl.exception.BallEventNotSupportedException;
import com.rsh.fcl.exception.GameAlreadyCompletedException;
import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.GameNotStartedException;
import com.rsh.fcl.model.BallEvent;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.PlayerType;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.ArrayList;
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
  private UserTeamRepository userTeamRepository;
  @Mock
  private BallEventRepository ballEventRepository;

  private GameService gameService;

  @BeforeEach
  void setUp() {
    gameService = new GameService(gameRepository, userTeamRepository, ballEventRepository);
    lenientSave();
  }

  private void lenientSave() {
    org.mockito.Mockito.lenient()
        .when(gameRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private List<PlayerRequest> squad(long base, String prefix) {
    return TestFixtures.playerRequests(base, prefix);
  }

  @Test
  void createGameBuildsTwoElevenPlayerSquads() {
    Game game = gameService.createGame("A", "B", 3, 5, squad(1, "A"), squad(12, "B"));

    assertThat(game.getTeams()).hasSize(2);
    assertThat(game.getAllPlayers()).hasSize(22);
    verify(gameRepository).save(game);
  }

  @Test
  void createGameRejectsNonPositiveTopK() {
    assertThatThrownBy(() -> gameService.createGame("A", "B", 0, 5, squad(1, "A"), squad(12, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Top K");
  }

  @Test
  void createGameRejectsNonPositiveOvers() {
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 0, squad(1, "A"), squad(12, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Overs");
  }

  @Test
  void createGameRejectsTeamWithoutElevenPlayers() {
    List<PlayerRequest> shortSquad = new ArrayList<>(squad(1, "A"));
    shortSquad.remove(0);
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, shortSquad, squad(12, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly 11 players");
  }

  @Test
  void createGameRejectsTeamWithoutWicketkeeper() {
    List<PlayerRequest> noKeeper = new ArrayList<>(squad(1, "A"));
    noKeeper.set(0, new PlayerRequest(1L, "A P1", PlayerType.BATTER));
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, noKeeper, squad(12, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one wicketkeeper");
  }

  @Test
  void createGameRejectsTeamWithTooFewBowlersAndAllrounders() {
    List<PlayerRequest> battingHeavy = new ArrayList<>(squad(1, "A"));
    for (int offset = 1; offset <= 7; offset++) {
      battingHeavy.set(offset, new PlayerRequest(1L + offset, "A P" + (offset + 1),
          PlayerType.BATTER));
    }
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, battingHeavy, squad(12, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 5 bowlers and all-rounders");
  }

  @Test
  void createGameRejectsDuplicateGlobalIdsAcrossTeams() {
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, squad(1, "A"), squad(1, "B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unique");
  }

  @Test
  void updateGameSyncsExistingTeamsInPlace() {
    Game existing = TestFixtures.game(5L, 3, 5);
    when(gameRepository.findById(5L)).thenReturn(Optional.of(existing));

    Game updated = gameService.updateGame(5L, "New Alpha", "New Beta", 4, 10,
        squad(1, "Alpha"), squad(12, "Beta"));

    assertThat(updated.getK()).isEqualTo(4);
    assertThat(updated.getOvers()).isEqualTo(10);
    List<String> teamNames = updated.getTeams().stream().map(t -> t.getName()).toList();
    assertThat(teamNames).containsExactly("New Alpha", "New Beta");
    assertThat(updated.getAllPlayers()).hasSize(22);
  }

  @Test
  void updateGameThrowsWhenGameMissing() {
    when(gameRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> gameService.updateGame(99L, "A", "B", 3, 5,
        squad(1, "A"), squad(12, "B")))
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
  void endGameRejectsCreatedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    assertThatThrownBy(() -> gameService.endGame(1L))
        .isInstanceOf(GameNotStartedException.class);
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

  private UserTeam userTeamWith(long... playerIds) {
    User user = TestFixtures.user(1L, "bob");
    Set<Player> players = new java.util.LinkedHashSet<>();
    for (long id : playerIds) {
      players.add(new Player(id, "P" + id, PlayerType.BATTER));
    }
    Game game = TestFixtures.game(1L, 3, 5);
    return new UserTeam(game, user, players);
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
    assertThatThrownBy(() -> gameService.play(1L, 1L, 12L, 4))
        .isInstanceOf(GameNotStartedException.class);
  }

  @Test
  void playRejectsCompletedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.COMPLETED);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> gameService.play(1L, 1L, 12L, 4))
        .isInstanceOf(GameAlreadyCompletedException.class);
  }

  @Test
  void playRejectsUnsupportedOutcome() {
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith(1L)));
    assertThatThrownBy(() -> gameService.play(1L, 1L, 12L, 3))
        .isInstanceOf(BallEventNotSupportedException.class);
  }

  @Test
  void playAwardsRunsToBatsmanOwners() {
    UserTeam batsmanOwner = userTeamWith(1L);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(batsmanOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, 1L, 12L, 6);

    assertThat(batsmanOwner.getPoints()).isEqualTo(3.0);
  }

  @Test
  void playPenalisesBowlerAndCountsWicket() {
    UserTeam bowlerOwner = userTeamWith(12L);
    Game game = inProgressGame();
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(bowlerOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, 1L, 12L, -1);

    assertThat(bowlerOwner.getPoints()).isEqualTo(4.0);
    assertThat(game.getWickets()).isEqualTo(1);
    assertThat(game.getBallsBowled()).isEqualTo(1);
  }

  @Test
  void playCompletesGameWhenInningsOver() {
    Game game = inProgressGame();
    game.setWickets(Game.ALL_OUT_WICKETS - 1);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith(12L)));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, 1L, 12L, -1);

    assertThat(game.getStatus()).isEqualTo(GameStatus.COMPLETED);
  }

  @Test
  void leaderboardReturnsTopKSortedByPoints() {
    Game game = TestFixtures.game(1L, 2, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

    UserTeam low = teamWithPoints("low", 1.0);
    UserTeam high = teamWithPoints("high", 9.0);
    UserTeam mid = teamWithPoints("mid", 5.0);
    when(userTeamRepository.findByGameId(1L)).thenReturn(List.of(low, high, mid));

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
    UserTeam owner = userTeamWith(1L);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(owner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, 1L, 12L, 1);

    ArgumentCaptor<List<UserTeam>> captor = ArgumentCaptor.forClass(List.class);
    verify(userTeamRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).containsExactly(owner);
    verify(ballEventRepository).save(any(BallEvent.class));
    verify(userTeamRepository, never()).deleteAll();
  }
}
