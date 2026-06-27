package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.GameNotFoundException;
import com.rsh.fcl.exception.UserNotFoundException;
import com.rsh.fcl.exception.UserTeamExistsException;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTeamServiceTest {

  @Mock
  private UserTeamRepository userTeamRepository;
  @Mock
  private GameRepository gameRepository;
  @Mock
  private UserRepository userRepository;

  private UserTeamService userTeamService;

  @BeforeEach
  void setUp() {
    userTeamService = new UserTeamService(userTeamRepository, gameRepository, userRepository);
  }

  /** Valid XI from the 1..22 roster: WK(1) + bowlers 2,3,4,5 + allrounder 6 + batters 9,10,11,12,17. */
  private List<Long> validSelection() {
    return new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 9L, 10L, 11L, 12L, 17L));
  }

  private void stubGameAndUser(Game game, User user) {
    when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
    when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
  }

  @Test
  void createTeamPersistsValidSelection() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    when(userTeamRepository.save(any(UserTeam.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserTeam team = userTeamService.createTeamForUser(1L, validSelection(), "bob");

    assertThat(team.getPlayers()).hasSize(11);
  }

  @Test
  void createTeamRejectsWhenGameMissing() {
    when(gameRepository.findById(9L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> userTeamService.createTeamForUser(9L, validSelection(), "bob"))
        .isInstanceOf(GameNotFoundException.class);
  }

  @Test
  void createTeamRejectsWhenGameAlreadyStarted() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.IN_PROGRESS);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, validSelection(), "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be modified");
  }

  @Test
  void createTeamRejectsUnknownUser() {
    Game game = TestFixtures.game(1L, 3, 5);
    when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
    when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, validSelection(), "ghost"))
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  void createTeamRejectsDuplicateTeamForUser() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(true);
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, validSelection(), "bob"))
        .isInstanceOf(UserTeamExistsException.class);
  }

  @Test
  void createTeamRejectsWrongSquadSize() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    List<Long> ten = validSelection();
    ten.remove(0);
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, ten, "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exactly 11 players");
  }

  @Test
  void createTeamRejectsPlayersOutsideRoster() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    List<Long> selection = validSelection();
    selection.set(10, 999L);
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, selection, "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("belong to the game roster");
  }

  @Test
  void createTeamRejectsDuplicatePlayerIds() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    List<Long> selection = new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 9L, 10L, 11L, 12L, 12L));
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, selection, "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("belong to the game roster");
  }

  @Test
  void createTeamRejectsMissingWicketkeeper() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    // ids 2-8 (bowlers/allrounders) + batters 9,10,11,20 - no wicketkeeper (1 or 12)
    List<Long> noKeeper = new ArrayList<>(List.of(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 20L));
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, noKeeper, "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least one wicketkeeper");
  }

  @Test
  void createTeamRejectsTooFewBowlersAndAllrounders() {
    Game game = TestFixtures.game(1L, 3, 5);
    User user = TestFixtures.user(2L, "bob");
    stubGameAndUser(game, user);
    when(userTeamRepository.existsByGameIdAndUser_UserName(1L, "bob")).thenReturn(false);
    // WK(1) + bowlers 2,3 + batters 9,10,11,20,21,22 + WK(12) -> only 2 bowler/allrounder
    List<Long> batsmanHeavy =
        new ArrayList<>(List.of(1L, 12L, 2L, 3L, 9L, 10L, 11L, 20L, 21L, 22L, 19L));
    assertThatThrownBy(() -> userTeamService.createTeamForUser(1L, batsmanHeavy, "bob"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least 5 bowlers and all-rounders");
  }

  @Test
  void getUserTeamForUserRejectsForeignAccess() {
    Game game = TestFixtures.game(1L, 3, 5);
    UserTeam team = new UserTeam(game, TestFixtures.user(2L, "bob"), java.util.Set.of());
    team.setId(5L);
    when(userTeamRepository.findById(5L)).thenReturn(Optional.of(team));
    assertThatThrownBy(() -> userTeamService.getUserTeamForUser(5L, "alice"))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void getUserTeamForUserReturnsOwnTeam() {
    Game game = TestFixtures.game(1L, 3, 5);
    UserTeam team = new UserTeam(game, TestFixtures.user(2L, "bob"), java.util.Set.of());
    team.setId(5L);
    when(userTeamRepository.findById(5L)).thenReturn(Optional.of(team));
    assertThat(userTeamService.getUserTeamForUser(5L, "bob")).isSameAs(team);
  }

  @Test
  void deleteUserTeamRequiresEditableGame() {
    Game game = TestFixtures.game(1L, 3, 5);
    game.setStatus(GameStatus.IN_PROGRESS);
    UserTeam team = new UserTeam(game, TestFixtures.user(2L, "bob"), java.util.Set.of());
    team.setId(5L);
    when(userTeamRepository.findById(5L)).thenReturn(Optional.of(team));
    assertThatThrownBy(() -> userTeamService.deleteUserTeam(5L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deleteUserTeamDelegatesWhenEditable() {
    Game game = TestFixtures.game(1L, 3, 5);
    UserTeam team = new UserTeam(game, TestFixtures.user(2L, "bob"), java.util.Set.of());
    team.setId(5L);
    when(userTeamRepository.findById(5L)).thenReturn(Optional.of(team));

    userTeamService.deleteUserTeam(5L);

    verify(userTeamRepository).delete(team);
  }
}
