package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.PlayerRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.ArrayList;
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
  private UserTeamRepository userTeamRepository;
  @Mock
  private BallEventRepository ballEventRepository;
  @Mock
  private PlayerRepository playerRepository;

  private GameService gameService;

  @BeforeEach
  void setUp() {
    lenient().when(playerRepository.existsById(anyString())).thenReturn(false);
    gameService = new GameService(gameRepository, userTeamRepository, ballEventRepository,
        new ReadablePlayerIdGenerator(playerRepository));
    lenient().when(gameRepository.save(any(Game.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private List<PlayerRequest> squad(String prefix) {
    return TestFixtures.playerRequests(prefix);
  }

  @Test
  void createGameGeneratesReadableIdsForTwoSquads() {
    Game game = gameService.createGame("A", "B", 3, 5, squad("A"), squad("B"));

    assertThat(game.getTeams()).hasSize(2);
    assertThat(game.getAllPlayers()).hasSize(22);
    assertThat(game.getAllPlayers())
        .allSatisfy(player -> assertThat(player.getGlobalUniqueId()).contains("_"));
    assertThat(game.getAllPlayers().stream().map(Player::getGlobalUniqueId).distinct().count())
        .isEqualTo(22);
    verify(gameRepository).save(game);
  }

  @Test
  void createGameRejectsNonPositiveTopK() {
    assertThatThrownBy(() -> gameService.createGame("A", "B", 0, 5, squad("A"), squad("B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Top K");
  }

  @Test
  void createGameRejectsNonPositiveOvers() {
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 0, squad("A"), squad("B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Overs");
  }

  @Test
  void createGameRejectsTeamWithoutElevenPlayers() {
    List<PlayerRequest> shortSquad = new ArrayList<>(squad("A"));
    shortSquad.remove(0);
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, shortSquad, squad("B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly 11 players");
  }

  @Test
  void createGameRejectsTeamWithoutWicketkeeper() {
    List<PlayerRequest> noKeeper = new ArrayList<>(squad("A"));
    noKeeper.set(0, new PlayerRequest("A P1", PlayerType.BATTER));
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, noKeeper, squad("B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one wicketkeeper");
  }

  @Test
  void createGameRejectsTeamWithTooFewBowlersAndAllrounders() {
    List<PlayerRequest> battingHeavy = new ArrayList<>(squad("A"));
    for (int offset = 1; offset <= 7; offset++) {
      battingHeavy.set(offset, new PlayerRequest("A P" + (offset + 1), PlayerType.BATTER));
    }
    assertThatThrownBy(() -> gameService.createGame("A", "B", 3, 5, battingHeavy, squad("B")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 5 bowlers and all-rounders");
  }

  @Test
  void updateGameSyncsExistingTeamsInPlace() {
    Game existing = TestFixtures.game(5L, 3, 5);
    List<String> originalIds =
        existing.getAllPlayers().stream().map(Player::getGlobalUniqueId).toList();
    when(gameRepository.findById(5L)).thenReturn(Optional.of(existing));

    Game updated = gameService.updateGame(5L, "New Alpha", "New Beta", 4, 10,
        squad("Alpha"), squad("Beta"));

    assertThat(updated.getK()).isEqualTo(4);
    assertThat(updated.getOvers()).isEqualTo(10);
    List<String> teamNames = updated.getTeams().stream().map(t -> t.getName()).toList();
    assertThat(teamNames).containsExactly("New Alpha", "New Beta");
    assertThat(updated.getAllPlayers()).hasSize(22);
    // ids are preserved across an update so existing user-team references stay valid
    assertThat(updated.getAllPlayers().stream().map(Player::getGlobalUniqueId).toList())
        .containsExactlyInAnyOrderElementsOf(originalIds);
  }

  @Test
  void updateGameThrowsWhenGameMissing() {
    when(gameRepository.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> gameService.updateGame(99L, "A", "B", 3, 5, squad("A"), squad("B")))
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

  private UserTeam userTeamWith(String... playerIds) {
    Set<Player> players = new LinkedHashSet<>();
    for (String id : playerIds) {
      players.add(new Player(id, "P-" + id, PlayerType.BATTER));
    }
    return new UserTeam(TestFixtures.game(1L, 3, 5), TestFixtures.user(1L, "bob"), players);
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
    assertThatThrownBy(() -> gameService.play(1L, "a1", "b1", 4))
        .isInstanceOf(GameNotStartedException.class);
  }

  @Test
  void playRejectsCompletedGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.COMPLETED);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> gameService.play(1L, "a1", "b1", 4))
        .isInstanceOf(GameAlreadyCompletedException.class);
  }

  @Test
  void playRejectsUnsupportedOutcome() {
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith("a1")));
    assertThatThrownBy(() -> gameService.play(1L, "a1", "b1", 3))
        .isInstanceOf(BallEventNotSupportedException.class);
  }

  @Test
  void playAwardsRunsToBatsmanOwners() {
    UserTeam batsmanOwner = userTeamWith("a1");
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(batsmanOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "a1", "b1", 6);

    assertThat(batsmanOwner.getPoints()).isEqualTo(3.0);
  }

  @Test
  void playPenalisesBowlerAndCountsWicket() {
    UserTeam bowlerOwner = userTeamWith("b1");
    Game game = inProgressGame();
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(bowlerOwner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "a1", "b1", -1);

    assertThat(bowlerOwner.getPoints()).isEqualTo(4.0);
    assertThat(game.getWickets()).isEqualTo(1);
    assertThat(game.getBallsBowled()).isEqualTo(1);
  }

  @Test
  void playCompletesGameWhenInningsOver() {
    Game game = inProgressGame();
    game.setWickets(Game.ALL_OUT_WICKETS - 1);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(userTeamWith("b1")));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "a1", "b1", -1);

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
    UserTeam owner = userTeamWith("a1");
    when(gameRepository.findById(1L)).thenReturn(Optional.of(inProgressGame()));
    when(userTeamRepository.findByGameIdForUpdate(1L)).thenReturn(List.of(owner));
    when(ballEventRepository.save(any(BallEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    gameService.play(1L, "a1", "b1", 1);

    ArgumentCaptor<List<UserTeam>> captor = ArgumentCaptor.forClass(List.class);
    verify(userTeamRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).containsExactly(owner);
    verify(ballEventRepository).save(any(BallEvent.class));
    verify(userTeamRepository, never()).deleteAll();
  }
}
