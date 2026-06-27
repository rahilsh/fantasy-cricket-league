package com.rsh.fcl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cricketers")
@Getter
@Setter
@NoArgsConstructor
public class Cricketer {

  @Id
  @Column(name = "global_unique_id")
  private String globalUniqueId;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CricketerType type;

  public Cricketer(String globalUniqueId, String name, CricketerType type) {
    this.globalUniqueId = globalUniqueId;
    this.name = name;
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Cricketer cricketer = (Cricketer) o;
    return Objects.equals(globalUniqueId, cricketer.globalUniqueId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(globalUniqueId);
  }
}
