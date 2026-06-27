package com.rsh.fcl.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_teams",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_teams_game_user", columnNames = {
        "game_id", "user_id"
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

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private double points;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_team_players",
      joinColumns = @JoinColumn(name = "user_team_id"),
      inverseJoinColumns = @JoinColumn(name = "player_id"))
  private Set<Player> players = new LinkedHashSet<>();

  public UserTeam(Game game, User user, Set<Player> players) {
    this.game = game;
    this.user = user;
    this.players = new LinkedHashSet<>(players);
  }

  public String getUserName() {
    return user != null ? user.getUserName() : null;
  }

  public boolean hasPlayer(long globalUniqueId) {
    return players.stream().anyMatch(player -> player.getGlobalUniqueId() == globalUniqueId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserTeam userTeam = (UserTeam) o;
    return Objects.equals(game != null ? game.getId() : null,
        userTeam.game != null ? userTeam.game.getId() : null)
        && Objects.equals(user != null ? user.getId() : null,
            userTeam.user != null ? userTeam.user.getId() : null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(game != null ? game.getId() : null, user != null ? user.getId() : null);
  }
}
