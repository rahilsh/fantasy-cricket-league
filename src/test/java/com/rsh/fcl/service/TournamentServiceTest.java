package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rsh.fcl.exception.CricketerNotFoundException;
import com.rsh.fcl.exception.ResourceNotFoundException;
import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.Team;
import com.rsh.fcl.model.Tournament;
import com.rsh.fcl.model.Tournament.TournamentStatus;
import com.rsh.fcl.repository.CricketerRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.repository.TournamentRepository;
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
class TournamentServiceTest {

  @Mock
  private TournamentRepository tournamentRepository;
  @Mock
  private TeamRepository teamRepository;
  @Mock
  private CricketerRepository cricketerRepository;

  private TournamentService tournamentService;

  @BeforeEach
  void setUp() {
    tournamentService = new TournamentService(tournamentRepository, teamRepository,
        cricketerRepository);
    lenient().when(tournamentRepository.save(any(Tournament.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private List<String> rosterIds() {
    List<String> ids = new ArrayList<>();
    for (int offset = 0; offset < 11; offset++) {
      ids.add(TestFixtures.cricketerId("a", offset));
    }
    return ids;
  }

  private void stubCricketers() {
    for (int offset = 0; offset < 11; offset++) {
      String id = TestFixtures.cricketerId("a", offset);
      lenient().when(cricketerRepository.findById(id))
          .thenReturn(Optional.of(TestFixtures.cricketer(id, TestFixtures.typeForOffset(offset))));
    }
  }

  @Test
  void createTournamentStartsInCreatedState() {
    Tournament tournament = tournamentService.createTournament("Premier League");
    assertThat(tournament.getName()).isEqualTo("Premier League");
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.CREATED);
  }

  @Test
  void onboardTeamAddsValidSquad() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
    stubCricketers();

    Team team = tournamentService.onboardTeam(1L, "Strikers", rosterIds());

    assertThat(team.getName()).isEqualTo("Strikers");
    assertThat(team.getCricketers()).hasSize(11);
    assertThat(tournament.getTeams()).contains(team);
    verify(teamRepository).save(team);
  }

  @Test
  void onboardTeamRejectsCompletedTournament() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    tournament.setStatus(TournamentStatus.COMPLETED);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

    assertThatThrownBy(() -> tournamentService.onboardTeam(1L, "Strikers", rosterIds()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("completed tournament");
  }

  @Test
  void onboardTeamRejectsDuplicateIds() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    List<String> ids = rosterIds();
    ids.set(10, ids.get(0));

    assertThatThrownBy(() -> tournamentService.onboardTeam(1L, "Strikers", ids))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unique");
  }

  @Test
  void onboardTeamRejectsMissingCricketer() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    when(cricketerRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tournamentService.onboardTeam(1L, "Strikers", rosterIds()))
        .isInstanceOf(CricketerNotFoundException.class);
  }

  @Test
  void onboardTeamRejectsCricketerAlreadyInActiveTournament() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    stubCricketers();
    when(teamRepository.findActiveTeamsForCricketer(eq(TestFixtures.cricketerId("a", 0)),
        eq(TournamentStatus.COMPLETED))).thenReturn(List.of(new Team("Busy")));

    assertThatThrownBy(() -> tournamentService.onboardTeam(1L, "Strikers", rosterIds()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already part of another active tournament");
  }

  @Test
  void onboardTeamRejectsInvalidComposition() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    // all batters -> no wicketkeeper
    for (int offset = 0; offset < 11; offset++) {
      String id = TestFixtures.cricketerId("a", offset);
      when(cricketerRepository.findById(id))
          .thenReturn(Optional.of(TestFixtures.cricketer(id, com.rsh.fcl.model.CricketerType.BATTER)));
    }

    assertThatThrownBy(() -> tournamentService.onboardTeam(1L, "Strikers", rosterIds()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wicketkeeper");
  }

  @Test
  void startTournamentMovesToInProgress() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    assertThat(tournamentService.startTournament(1L).getStatus())
        .isEqualTo(TournamentStatus.IN_PROGRESS);
  }

  @Test
  void startTournamentRejectsCompleted() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    tournament.setStatus(TournamentStatus.COMPLETED);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    assertThatThrownBy(() -> tournamentService.startTournament(1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void endTournamentCompletesIt() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    assertThat(tournamentService.endTournament(1L).getStatus())
        .isEqualTo(TournamentStatus.COMPLETED);
  }

  @Test
  void updateTournamentChangesName() {
    Tournament tournament = TestFixtures.tournament(1L, "Old");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    assertThat(tournamentService.updateTournament(1L, "New").getName()).isEqualTo("New");
  }

  @Test
  void getTournamentThrowsWhenMissing() {
    when(tournamentRepository.findById(9L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> tournamentService.getTournament(9L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteTournamentDelegates() {
    Tournament tournament = TestFixtures.tournament(1L, "PL");
    when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
    tournamentService.deleteTournament(1L);
    verify(tournamentRepository).delete(tournament);
  }
}
