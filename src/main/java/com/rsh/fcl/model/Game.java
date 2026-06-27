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
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

  public static final int ALL_OUT_WICKETS = 10;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameStatus status = GameStatus.CREATED;

  @Column(name = "top_k", nullable = false)
  private int k = 3;

  @Column(nullable = false)
  private int overs;

  @Column(name = "balls_bowled", nullable = false)
  private int ballsBowled = 0;

  @Column(name = "wickets", nullable = false)
  private int wickets = 0;

  @OneToMany(
      mappedBy = "game",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("id ASC")
  private Set<Team> teams = new LinkedHashSet<>();

  public Game(int k, int overs) {
    this.k = k;
    this.overs = overs;
  }

  public int totalBalls() {
    return overs * 6;
  }

  public void addTeam(Team team) {
    team.setGame(this);
    teams.add(team);
  }

  public List<Player> getAllPlayers() {
    return teams.stream()
        .flatMap(team -> team.getPlayers().stream())
        .toList();
  }

  public boolean isInningsOver() {
    return ballsBowled >= totalBalls() || wickets >= ALL_OUT_WICKETS;
  }

  public enum GameStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED
  }
}
