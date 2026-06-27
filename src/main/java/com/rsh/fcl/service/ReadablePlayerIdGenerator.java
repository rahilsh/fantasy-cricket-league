package com.rsh.fcl.service;

import com.rsh.fcl.repository.PlayerRepository;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Generates Docker-style {@code adjective_noun} player ids that are globally unique. */
@Component
public class ReadablePlayerIdGenerator {

  private static final List<String> ADJECTIVES = List.of(
      "agile", "bold", "brave", "calm", "clever", "cool", "eager", "fancy", "fearless", "fierce",
      "gentle", "happy", "jolly", "keen", "lively", "lucky", "mighty", "noble", "proud", "quick",
      "rapid", "sharp", "shiny", "silent", "smart", "snappy", "spry", "sturdy", "swift", "vivid");

  private static final List<String> NOUNS = List.of(
      "ace", "bat", "bolt", "comet", "cobra", "eagle", "falcon", "hawk", "jaguar", "kestrel",
      "lion", "lynx", "maverick", "ninja", "otter", "panther", "phoenix", "puma", "raptor", "rhino",
      "rocket", "shark", "stallion", "tiger", "titan", "viper", "wizard", "wolf", "yak", "zephyr");

  private final PlayerRepository playerRepository;

  public ReadablePlayerIdGenerator(PlayerRepository playerRepository) {
    this.playerRepository = playerRepository;
  }

  /**
   * Returns a readable id not present in {@code reserved} nor already persisted. A numeric suffix is
   * appended as a fallback to guarantee uniqueness when the {@code adjective_noun} space is busy.
   */
  public String generateUnique(Set<String> reserved) {
    for (int attempt = 0; attempt < 100; attempt++) {
      String candidate = randomName();
      if (isAvailable(candidate, reserved)) {
        return candidate;
      }
    }
    String candidate;
    do {
      candidate = randomName() + "_" + ThreadLocalRandom.current().nextInt(1000, 1_000_000);
    } while (!isAvailable(candidate, reserved));
    return candidate;
  }

  private boolean isAvailable(String candidate, Set<String> reserved) {
    return !reserved.contains(candidate) && !playerRepository.existsById(candidate);
  }

  private static String randomName() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return ADJECTIVES.get(random.nextInt(ADJECTIVES.size()))
        + "_" + NOUNS.get(random.nextInt(NOUNS.size()));
  }
}
