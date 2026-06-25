package com.rsh.fcl.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_teams",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_teams_game_user", columnNames = {
        "game_id", "user_name"
    })
)
@Getter
@Setter
@NoArgsConstructor
public class UserTeam {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private Game game;

  @Column(name = "user_name", nullable = false)
  private String userName;

  @Column(nullable = false)
  private double points;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_team_players", joinColumns = @JoinColumn(name = "user_team_id"))
  @Column(name = "player_id", nullable = false)
  private Set<Integer> players = new HashSet<>();

  public UserTeam(Game game, String userName, List<Integer> players) {
    this.game = game;
    this.userName = userName;
    this.players = new HashSet<>(players);
  }

  public boolean hasPlayer(int playerId) {
    return players.contains(playerId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserTeam userTeam = (UserTeam) o;
    return Objects.equals(game != null ? game.getId() : null,
        userTeam.game != null ? userTeam.game.getId() : null)
        && Objects.equals(userName, userTeam.userName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(game != null ? game.getId() : null, userName);
  }
}
