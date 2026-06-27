package com.rsh.fcl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tournament_id", nullable = false)
  private Tournament tournament;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "team_cricketers",
      joinColumns = @JoinColumn(name = "team_id"),
      inverseJoinColumns = @JoinColumn(name = "cricketer_id"))
  @OrderBy("globalUniqueId ASC")
  private Set<Cricketer> cricketers = new LinkedHashSet<>();

  public Team(String name) {
    this.name = name;
  }

  public void addCricketer(Cricketer cricketer) {
    cricketers.add(cricketer);
  }

  public boolean hasCricketer(String globalUniqueId) {
    return cricketers.stream()
        .anyMatch(cricketer -> cricketer.getGlobalUniqueId().equals(globalUniqueId));
  }
}
