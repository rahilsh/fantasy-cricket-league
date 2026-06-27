package com.rsh.fcl;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rsh.fcl.model.Game;
import com.rsh.fcl.model.Player;
import com.rsh.fcl.model.PlayerType;
import com.rsh.fcl.model.User;
import com.rsh.fcl.model.UserTeam;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.PlayerRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GameFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BallEventRepository ballEventRepository;

  @Autowired
  private UserTeamRepository userTeamRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GameRepository gameRepository;

  @Autowired
  private PlayerRepository playerRepository;

  @BeforeEach
  void cleanup() {
    ballEventRepository.deleteAll();
    userTeamRepository.deleteAll();
    playerRepository.deleteAll();
    gameRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void simulatesGameEndToEndThroughRestApis() throws Exception {
    Roster roster = createGame("IND", "PAK", 5, 10);

    Map<String, List<String>> selections = new TreeMap<>();
    for (int i = 1; i <= 5; i++) {
      String user = "user" + i;
      createUser(user);
      List<String> xi = roster.validXi(i);
      createUserTeam(roster.gameId, user, xi);
      selections.put(user, xi);
    }

    mockMvc.perform(post("/api/games/{id}/start", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc.perform(get("/api/games/{id}", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team1Players", hasSize(11)))
        .andExpect(jsonPath("$.team2Players", hasSize(11)));

    int[][] balls = {
        {0, 11, 6}, {1, 12, -1}, {2, 13, 4}, {3, 14, 2}, {4, 15, 1},
        {5, 16, 6}, {6, 17, 4}, {7, 18, 2}, {8, 19, 1}, {9, 20, 6},
        {10, 21, 4}, {1, 0, 2},
    };
    Map<String, Double> expectedPoints = newScore(selections.keySet());
    for (int[] ball : balls) {
      String batsman = roster.all.get(ball[0]);
      String bowler = roster.all.get(ball[1]);
      int outcome = ball[2];
      play(roster.gameId, batsman, bowler, outcome);
      applyScore(expectedPoints, selections, batsman, bowler, outcome);
    }

    List<Map.Entry<String, Double>> expectedTop = expectedPoints.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(5)
        .toList();

    var result = mockMvc.perform(get("/api/games/{id}/leaderboard", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)));
    for (int rank = 0; rank < expectedTop.size(); rank++) {
      result.andExpect(jsonPath("$[" + rank + "].userName").value(expectedTop.get(rank).getKey()))
          .andExpect(jsonPath("$[" + rank + "].points").value(expectedTop.get(rank).getValue()));
    }

    mockMvc.perform(post("/api/games/{id}/end", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(roster.all.get(0), roster.all.get(1), 6)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + roster.gameId + " already completed"));
  }

  @Test
  void supportsCrudForGameUserAndUserTeam() throws Exception {
    long userId = createUser("rahil");
    mockMvc.perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("rahil"));

    mockMvc.perform(put("/api/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"captain\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("captain"));

    Roster roster = createGame("AUS", "ENG", 2, 5);
    mockMvc.perform(put("/api/games/{id}", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody("AUS", "NZ", 1, 8)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team2").value("NZ"))
        .andExpect(jsonPath("$.k").value(1))
        .andExpect(jsonPath("$.overs").value(8));

    // roster ids are stable across the update, so the captain's XI is still valid
    long userTeamId = createUserTeam(roster.gameId, "captain", roster.validXi(0));
    mockMvc.perform(put("/api/user-teams/{id}", userTeamId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "captain", roster.validXi(2), 9.5)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points").value(9.5))
        .andExpect(jsonPath("$.players", hasSize(11)));

    mockMvc.perform(delete("/api/user-teams/{id}", userTeamId))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/users/{id}", userId))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/games/{id}", roster.gameId))
        .andExpect(status().isNoContent());
  }

  @Test
  void supportsPaginationAndSortingForListApis() throws Exception {
    createUser("alice");
    createUser("charlie");
    createUser("bob");

    mockMvc.perform(get("/api/users?page=0&size=2&sort=userName,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].userName").value("charlie"))
        .andExpect(jsonPath("$.content[1].userName").value("bob"))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void blocksUserTeamModificationAfterGameStart() throws Exception {
    Roster roster = createGame("IND", "AUS", 3, 5);
    createUser("captain");
    long userTeamId = createUserTeam(roster.gameId, "captain", roster.validXi(0));

    mockMvc.perform(post("/api/games/{id}/start", roster.gameId))
        .andExpect(status().isOk());

    mockMvc.perform(put("/api/user-teams/{id}", userTeamId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "captain", roster.validXi(3), 0.0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("User teams cannot be modified after game has started"));
  }

  @Test
  void returnsHelpfulErrorsForInvalidRequests() throws Exception {
    mockMvc.perform(post("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"team1\":\"\",\"team2\":\"PAK\",\"k\":0}"))
        .andExpect(status().isBadRequest());

    mockMvc.perform(post("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody("IND", "PAK", 3, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("overs must not be null"));

    Roster roster = createGame("IND", "PAK", 3, 5);

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(roster.all.get(0), roster.all.get(1), 6)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + roster.gameId + " not yet started"));

    mockMvc.perform(get("/api/games/{id}", 9999))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/games/{id}/start", roster.gameId))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":\"\",\"bowler\":\"" + roster.all.get(1) + "\",\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("batsman must not be blank"));

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":\"" + roster.all.get(0) + "\",\"bowler\":\""
                + roster.all.get(1) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("outcome must not be null"));

    createUser("fielder");
    List<String> withBlank = new ArrayList<>(roster.validXi(0));
    withBlank.set(0, "");
    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "fielder", withBlank, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("players[0] must not be blank"));

    List<String> twelve = new ArrayList<>(roster.validXi(0));
    twelve.add(roster.all.get(21));
    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "fielder", twelve, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("players must contain exactly 11 players"));

    Roster rosterGame = createGame("NZ", "SL", 3, 5);
    List<String> outsideRoster = new ArrayList<>(rosterGame.validXi(0));
    outsideRoster.set(10, "ghost_player");
    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(rosterGame.gameId, "fielder", outsideRoster, null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Selected players must belong to the game roster"));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(rosterGame.gameId, "fielder", rosterGame.withoutWicketkeeper(),
                null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("User team must contain at least one wicketkeeper"));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(rosterGame.gameId, "fielder", rosterGame.tooFewBowlers(), null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "User team must contain at least 5 bowlers and all-rounders"));

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(roster.all.get(0), roster.all.get(1), 3)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Ball event outcome 3 is not supported"));
  }

  @Test
  void coversNotFoundAndDuplicateBranches() throws Exception {
    Roster roster = createGame("IND", "PAK", 3, 5);

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "ghost", roster.validXi(0), null)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User ghost does not exist"));

    createUser("user1");
    createUserTeam(roster.gameId, "user1", roster.validXi(0));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(roster.gameId, "user1", roster.validXi(2), null)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "User team for user user1 already for game " + roster.gameId));

    mockMvc.perform(get("/api/user-teams?gameId=9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("gameId 9999 does not exist"));

    mockMvc.perform(get("/api/users/9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User 9999 does not exist"));

    Roster emptyGame = createGame("SL", "BAN", 3, 5);
    mockMvc.perform(get("/api/user-teams?gameId={gameId}", emptyGame.gameId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(
            "UserTeam does not exists for game " + emptyGame.gameId));
  }

  @Test
  void userTeamEqualityUsesGameAndUser() {
    Game game = new Game(3, 5);
    game.setId(1L);
    Game anotherGame = new Game(3, 5);
    anotherGame.setId(2L);
    User user = new User("user1");
    user.setId(1L);
    User sameUserId = new User("user1-renamed");
    sameUserId.setId(1L);
    User anotherUser = new User("user1");
    anotherUser.setId(2L);

    Set<Player> players = new LinkedHashSet<>();
    players.add(new Player("brave_lion", "player1", PlayerType.BATTER));
    players.add(new Player("calm_otter", "player2", PlayerType.BOWLER));

    UserTeam userTeam = new UserTeam(game, user, players);
    UserTeam sameUserTeam = new UserTeam(game, sameUserId, players);
    UserTeam differentUserTeam = new UserTeam(game, anotherUser, players);
    UserTeam differentGameTeam = new UserTeam(anotherGame, user, players);

    assertEquals(userTeam, sameUserTeam);
    assertEquals(userTeam.hashCode(), sameUserTeam.hashCode());
    assertNotEquals(userTeam, differentUserTeam);
    assertNotEquals(userTeam, differentGameTeam);
    assertNotEquals(userTeam, null);
    assertNotEquals(userTeam, "user1");
  }

  @Test
  void automaticallyEndsGameWhenAllOversAreCompleted() throws Exception {
    Roster roster = createGame("IND", "PAK", 3, 1);
    createUser("user1");
    createUserTeam(roster.gameId, "user1", roster.validXi(0));

    mockMvc.perform(post("/api/games/{id}/start", roster.gameId))
        .andExpect(status().isOk());

    for (int ball = 1; ball <= 5; ball++) {
      play(roster.gameId, roster.all.get(0), roster.all.get(1), 1);
    }

    mockMvc.perform(get("/api/games/{id}", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.ballsBowled").value(5));

    play(roster.gameId, roster.all.get(0), roster.all.get(1), 1);

    mockMvc.perform(get("/api/games/{id}", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.ballsBowled").value(6));

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(roster.all.get(0), roster.all.get(1), 6)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + roster.gameId + " already completed"));
  }

  @Test
  void automaticallyEndsGameWhenTenWicketsFall() throws Exception {
    Roster roster = createGame("IND", "PAK", 3, 20);
    createUser("user1");
    createUserTeam(roster.gameId, "user1", roster.validXi(0));

    mockMvc.perform(post("/api/games/{id}/start", roster.gameId))
        .andExpect(status().isOk());

    for (int wicket = 1; wicket <= 9; wicket++) {
      play(roster.gameId, roster.all.get(0), roster.all.get(1), -1);
    }

    mockMvc.perform(get("/api/games/{id}", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.wickets").value(9));

    play(roster.gameId, roster.all.get(0), roster.all.get(1), -1);

    mockMvc.perform(get("/api/games/{id}", roster.gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.wickets").value(10))
        .andExpect(jsonPath("$.ballsBowled").value(10));

    mockMvc.perform(post("/api/games/{id}/plays", roster.gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(roster.all.get(0), roster.all.get(1), 6)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + roster.gameId + " already completed"));
  }

  // ---- helpers ----

  private Roster createGame(String team1, String team2, int k, int overs) throws Exception {
    String json = mockMvc.perform(post("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(team1, team2, k, overs)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return Roster.from(objectMapper.readTree(json));
  }

  private long createUser(String userName) throws Exception {
    return idFrom(mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"" + userName + "\"}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private long createUserTeam(long gameId, String userName, List<String> players) throws Exception {
    return idFrom(mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(gameId, userName, players, null)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private void play(long gameId, String batsman, String bowler, int outcome) throws Exception {
    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(playBody(batsman, bowler, outcome)))
        .andExpect(status().isOk());
  }

  private long idFrom(String json) {
    return objectMapper.readTree(json).get("id").asLong();
  }

  private String gameRequestBody(String team1, String team2, Integer k, Integer overs) {
    StringBuilder builder = new StringBuilder();
    builder.append("{\"team1\":\"").append(team1)
        .append("\",\"team2\":\"").append(team2)
        .append("\",\"k\":").append(k);
    builder.append(overs == null ? ",\"overs\":null" : ",\"overs\":" + overs);
    builder.append(",\"team1Players\":").append(playersJson(team1))
        .append(",\"team2Players\":").append(playersJson(team2))
        .append("}");
    return builder.toString();
  }

  private String playersJson(String teamName) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < 11; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append("{\"name\":\"").append(teamName).append("-").append(i + 1)
          .append("\",\"type\":\"").append(playerType(i)).append("\"}");
    }
    return builder.append(']').toString();
  }

  private String userTeamBody(long gameId, String userName, List<String> players, Double points) {
    StringBuilder builder = new StringBuilder("{\"gameId\":").append(gameId)
        .append(",\"userName\":\"").append(userName)
        .append("\",\"players\":").append(jsonArray(players));
    if (points != null) {
      builder.append(",\"points\":").append(points);
    }
    return builder.append('}').toString();
  }

  private String playBody(String batsman, String bowler, int outcome) {
    return "{\"batsman\":\"" + batsman + "\",\"bowler\":\"" + bowler + "\",\"outcome\":" + outcome
        + "}";
  }

  private static String jsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append('"').append(values.get(i)).append('"');
    }
    return builder.append(']').toString();
  }

  private static String playerType(int offset) {
    if (offset == 0) {
      return "WICKETKEEPER";
    }
    if (offset <= 4) {
      return "BOWLER";
    }
    if (offset <= 7) {
      return "ALLROUNDER";
    }
    return "BATTER";
  }

  private static Map<String, Double> newScore(Set<String> users) {
    Map<String, Double> score = new TreeMap<>();
    users.forEach(user -> score.put(user, 0.0));
    return score;
  }

  private static void applyScore(Map<String, Double> score, Map<String, List<String>> selections,
      String batsman, String bowler, int outcome) {
    double batsmanDelta;
    double bowlerDelta;
    switch (outcome) {
      case 1 -> { batsmanDelta = 0.5; bowlerDelta = 0.0; }
      case 2 -> { batsmanDelta = 1.0; bowlerDelta = -0.5; }
      case 4 -> { batsmanDelta = 2.0; bowlerDelta = -1.0; }
      case 6 -> { batsmanDelta = 3.0; bowlerDelta = -2.0; }
      case -1 -> { batsmanDelta = -2.0; bowlerDelta = 4.0; }
      default -> throw new IllegalStateException("unsupported outcome " + outcome);
    }
    selections.forEach((user, xi) -> {
      double delta = 0.0;
      if (xi.contains(batsman)) {
        delta += batsmanDelta;
      }
      if (xi.contains(bowler)) {
        delta += bowlerDelta;
      }
      score.merge(user, delta, Double::sum);
    });
  }

  /** Captures a created game's roster so tests can build valid (and invalid) selections. */
  private static final class Roster {
    private final long gameId;
    private final List<String> all = new ArrayList<>();
    private final List<String> wicketkeepers = new ArrayList<>();
    private final List<String> pacemen = new ArrayList<>();
    private final List<String> batters = new ArrayList<>();

    private Roster(long gameId) {
      this.gameId = gameId;
    }

    static Roster from(JsonNode game) {
      Roster roster = new Roster(game.get("id").asLong());
      roster.ingest(game.get("team1Players"));
      roster.ingest(game.get("team2Players"));
      return roster;
    }

    private void ingest(JsonNode players) {
      for (JsonNode player : players) {
        String id = player.get("globalUniqueId").asString();
        all.add(id);
        switch (player.get("type").asString()) {
          case "WICKETKEEPER" -> wicketkeepers.add(id);
          case "BOWLER", "ALLROUNDER" -> pacemen.add(id);
          default -> batters.add(id);
        }
      }
    }

    List<String> validXi(int variant) {
      LinkedHashSet<String> selection = new LinkedHashSet<>();
      selection.add(wicketkeepers.get(0));
      for (int i = 0; i < 5; i++) {
        selection.add(pacemen.get((variant + i) % pacemen.size()));
      }
      for (String id : all) {
        if (selection.size() == 11) {
          break;
        }
        selection.add(id);
      }
      return new ArrayList<>(selection);
    }

    List<String> withoutWicketkeeper() {
      List<String> selection = new ArrayList<>();
      selection.addAll(pacemen.subList(0, 8));
      selection.addAll(batters.subList(0, 3));
      return selection;
    }

    List<String> tooFewBowlers() {
      List<String> selection = new ArrayList<>();
      selection.addAll(wicketkeepers);
      selection.addAll(batters.subList(0, 6));
      selection.addAll(pacemen.subList(0, 3));
      return selection;
    }
  }
}
