package com.rsh.fcl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ball_events")
@Getter
@Setter
@NoArgsConstructor
public class BallEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @Column(nullable = false)
  private String batsman;

  @Column(nullable = false)
  private String bowler;

  @Column(nullable = false)
  private int score;

  public BallEvent(Game game, String batsman, String bowler, int score) {
    this.game = game;
    this.batsman = batsman;
    this.bowler = bowler;
    this.score = score;
  }
}
