package com.rsh.fcl.service;

import com.rsh.fcl.model.Cricketer;
import com.rsh.fcl.model.CricketerType;
import java.util.Collection;

final class SquadRules {

  static final int SQUAD_SIZE = 11;
  static final int MIN_WICKETKEEPERS = 1;
  static final int MIN_BOWLERS_AND_ALLROUNDERS = 5;

  private SquadRules() {
  }

  static void validateComposition(Collection<Cricketer> cricketers, String subject) {
    if (cricketers.size() != SQUAD_SIZE) {
      throw new IllegalArgumentException(
          subject + " must contain exactly " + SQUAD_SIZE + " cricketers");
    }
    long wicketkeepers = cricketers.stream()
        .filter(cricketer -> cricketer.getType() == CricketerType.WICKETKEEPER)
        .count();
    if (wicketkeepers < MIN_WICKETKEEPERS) {
      throw new IllegalArgumentException(subject + " must contain at least one wicketkeeper");
    }
    long bowlersAndAllrounders = cricketers.stream()
        .filter(cricketer -> cricketer.getType() == CricketerType.BOWLER
            || cricketer.getType() == CricketerType.ALLROUNDER)
        .count();
    if (bowlersAndAllrounders < MIN_BOWLERS_AND_ALLROUNDERS) {
      throw new IllegalArgumentException(
          subject + " must contain at least 5 bowlers and all-rounders");
    }
  }
}
