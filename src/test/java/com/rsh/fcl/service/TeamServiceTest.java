package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Game.GameStatus;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import com.rsh.fcl.repository.CricketerRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.support.TestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

  @Mock
  private TeamRepository teamRepository;
  @Mock
  private CricketerRepository cricketerRepository;
  @Mock
  private GameRepository gameRepository;

  private TeamService teamService;

  @BeforeEach
  void setUp() {
    teamService = new TeamService(teamRepository, cricketerRepository, gameRepository);
    lenient().when(teamRepository.save(any(Team.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Team teamInProgress() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    Team team = TestFixtures.team("Strikers", "a");
    team.setId(1L);
    tournament.addTeam(team);
    return team;
  }

  @Test
  void addCricketerRejectsWhenTeamFull() {
    Team team = teamInProgress();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(() -> teamService.addCricketer(1L, "abc_new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already has 11");
  }

  @Test
  void addCricketerAddsToTeamWithSpace() {
    Team team = teamInProgress();
    team.getCricketers().remove(team.getCricketers().iterator().next());
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(cricketerRepository.findById("abc_new"))
        .thenReturn(Optional.of(TestFixtures.cricketer("abc_new", CricketerType.BATTER)));

    Team updated = teamService.addCricketer(1L, "abc_new");

    assertThat(updated.hasCricketer("abc_new")).isTrue();
    assertThat(updated.getCricketers()).hasSize(11);
  }

  @Test
  void addCricketerRejectsWhenTournamentNotInProgress() {
    Team team = teamInProgress();
    team.getTournament().setStatus(TournamentStatus.CREATED);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(() -> teamService.addCricketer(1L, "abc_new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("while the tournament is in progress");
  }

  @Test
  void addCricketerRejectsWhenTeamInLiveGame() {
    Team team = teamInProgress();
    team.getCricketers().remove(team.getCricketers().iterator().next());
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(gameRepository.findByStatusAndTeam(eq(GameStatus.IN_PROGRESS), eq(1L)))
        .thenReturn(List.of(new Game()));

    assertThatThrownBy(() -> teamService.addCricketer(1L, "abc_new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("while a match is in progress");
  }

  @Test
  void addCricketerRejectsDuplicate() {
    Team team = teamInProgress();
    team.getCricketers().remove(team.getCricketers().iterator().next());
    String existing = TestFixtures.cricketerId("a", 5);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(() -> teamService.addCricketer(1L, existing))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already in the team");
  }

  @Test
  void addCricketerRejectsWhenCricketerInAnotherActiveTeam() {
    Team team = teamInProgress();
    team.getCricketers().remove(team.getCricketers().iterator().next());
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(cricketerRepository.findById("abc_new"))
        .thenReturn(Optional.of(TestFixtures.cricketer("abc_new", CricketerType.BATTER)));
    when(teamRepository.findActiveTeamsForCricketer(eq("abc_new"), eq(TournamentStatus.COMPLETED)))
        .thenReturn(List.of(new Team("Busy")));

    assertThatThrownBy(() -> teamService.addCricketer(1L, "abc_new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already part of another active tournament");
  }

  @Test
  void replaceCricketerSwapsAndKeepsCompositionValid() {
    Team team = teamInProgress();
    String outgoing = TestFixtures.cricketerId("a", 8); // a batter
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(cricketerRepository.findById("abc_new"))
        .thenReturn(Optional.of(TestFixtures.cricketer("abc_new", CricketerType.BATTER)));

    Team updated = teamService.replaceCricketer(1L, outgoing, "abc_new");

    assertThat(updated.hasCricketer(outgoing)).isFalse();
    assertThat(updated.hasCricketer("abc_new")).isTrue();
    assertThat(updated.getCricketers()).hasSize(11);
  }

  @Test
  void replaceCricketerRejectsWhenOutgoingNotInTeam() {
    Team team = teamInProgress();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(() -> teamService.replaceCricketer(1L, "abc_zzz", "abc_new"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not in the team");
  }

  @Test
  void removeCricketerDropsFromTeam() {
    Team team = teamInProgress();
    String target = TestFixtures.cricketerId("a", 8);
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    Team updated = teamService.removeCricketer(1L, target);

    assertThat(updated.hasCricketer(target)).isFalse();
    assertThat(updated.getCricketers()).hasSize(10);
    verify(teamRepository).save(team);
  }

  @Test
  void removeCricketerRejectsWhenNotInTeam() {
    Team team = teamInProgress();
    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    assertThatThrownBy(() -> teamService.removeCricketer(1L, "abc_zzz"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("is not in the team");
  }

  @Test
  void getTeamThrowsWhenMissing() {
    when(teamRepository.findById(9L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> teamService.getTeam(9L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
