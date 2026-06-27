package com.rsh.fcl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

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

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "team1_id", nullable = false)
  private Team team1;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "team2_id", nullable = false)
  private Team team2;

  public Game(Tournament tournament, Team team1, Team team2, int k, int overs) {
    this.tournament = tournament;
    this.team1 = team1;
    this.team2 = team2;
    this.k = k;
    this.overs = overs;
  }

  public int totalBalls() {
    return overs * 6;
  }

  public List<Cricketer> getAllCricketers() {
    List<Cricketer> all = new ArrayList<>(team1.getCricketers());
    all.addAll(team2.getCricketers());
    return all;
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
