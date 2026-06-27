package com.rsh.fcl.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
public class Tournament {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TournamentStatus status = TournamentStatus.CREATED;

  @OneToMany(
      mappedBy = "tournament",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private Set<Team> teams = new LinkedHashSet<>();

  public Tournament(String name) {
    this.name = name;
  }

  public void addTeam(Team team) {
    team.setTournament(this);
    teams.add(team);
  }

  public boolean isActive() {
    return status != TournamentStatus.COMPLETED;
  }

  public enum TournamentStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED
  }
}
