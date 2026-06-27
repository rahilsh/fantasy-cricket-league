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
import java.util.LinkedHashSet;
import java.util.Set;
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

  private long nextPlayerId;

  @BeforeEach
  void cleanup() {
    ballEventRepository.deleteAll();
    userTeamRepository.deleteAll();
    playerRepository.deleteAll();
    gameRepository.deleteAll();
    userRepository.deleteAll();
    nextPlayerId = 1;
  }

  @Test
  void simulatesGameEndToEndThroughRestApis() throws Exception {
    long gameId = createGame("IND", "PAK", 5, 10);
    createUser("user1");
    createUser("user2");
    createUser("user3");
    createUser("user4");
    createUser("user5");

    createUserTeam(gameId, "user1", "[1,12,2,13,3,14,7,18,8,19,10]");
    createUserTeam(gameId, "user2", "[1,2,3,4,15,16,7,18,8,19,9]");
    createUserTeam(gameId, "user3", "[1,2,13,3,4,14,7,18,8,19,9]");
    createUserTeam(gameId, "user4", "[5,12,2,13,3,14,7,18,8,20,9]");
    createUserTeam(gameId, "user5", "[6,12,2,13,3,14,7,18,8,19,9]");

    mockMvc.perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc.perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team1Players", hasSize(11)))
        .andExpect(jsonPath("$.team2Players", hasSize(11)));

    play(gameId, 1, 22, 6);
    play(gameId, 2, 21, -1);
    play(gameId, 3, 20, 4);
    play(gameId, 4, 19, -1);
    play(gameId, 5, 18, 1);
    play(gameId, 6, 18, 2);
    play(gameId, 12, 7, 1);
    play(gameId, 13, 8, 6);
    play(gameId, 14, 9, -1);
    play(gameId, 15, 10, 2);
    play(gameId, 16, 11, -1);
    play(gameId, 17, 11, 4);

    mockMvc.perform(get("/api/games/{id}/leaderboard", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].userName").value("user5"))
        .andExpect(jsonPath("$[0].points").value(8.0))
        .andExpect(jsonPath("$[1].userName").value("user3"))
        .andExpect(jsonPath("$[1].points").value(7.5))
        .andExpect(jsonPath("$[2].userName").value("user1"))
        .andExpect(jsonPath("$[2].points").value(5.5));

    mockMvc.perform(post("/api/games/{id}/end", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2,\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + gameId + " already completed"));
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

    long gameId = createGame("AUS", "ENG", 2, 5);
    mockMvc.perform(put("/api/games/{id}", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody("AUS", "NZ", 1, 8, 1, 12)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team2").value("NZ"))
        .andExpect(jsonPath("$.k").value(1))
        .andExpect(jsonPath("$.overs").value(8));

    long userTeamId = createUserTeam(gameId, "captain", "[1,2,3,4,5,6,7,8,9,10,11]");
    mockMvc.perform(put("/api/user-teams/{id}", userTeamId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"captain\",\"players\":[12,13,14,15,16,17,18,19,20,21,22],\"points\":9.5}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.points").value(9.5))
        .andExpect(jsonPath("$.players", hasSize(11)));

    mockMvc.perform(delete("/api/user-teams/{id}", userTeamId))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/users/{id}", userId))
        .andExpect(status().isNoContent());
    mockMvc.perform(delete("/api/games/{id}", gameId))
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
    long gameId = createGame("IND", "AUS", 3, 5);
    createUser("captain");
    long userTeamId = createUserTeam(gameId, "captain", "[1,2,3,4,5,6,7,8,9,10,11]");

    mockMvc.perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk());

    mockMvc.perform(put("/api/user-teams/{id}", userTeamId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"captain\",\"players\":[12,13,14,15,16,17,18,19,20,21,22],\"points\":0.0}"))
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
            .content(gameRequestBody("IND", "PAK", 3, null, 1, 12)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("overs must not be null"));

    long gameId = createGame("IND", "PAK", 3, 5);

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2,\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + gameId + " not yet started"));

    mockMvc.perform(get("/api/games/{id}", 9999))
        .andExpect(status().isNotFound());

    mockMvc.perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":-1,\"bowler\":2,\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("batsman must be greater than 0"));

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("outcome must not be null"));

    createUser("fielder");
    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"fielder\",\"players\":[-5,2,3,4,5,6,7,8,9,10,11]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("players[0] must be greater than 0"));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"fielder\",\"players\":[1,2,3,4,5,6,7,8,9,10,11,12]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("players must contain exactly 11 players"));

    long rosterGameId = createGame("NZ", "SL", 3, 5);
    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + rosterGameId
                + ",\"userName\":\"fielder\",\"players\":[1,2,3,4,5,6,7,8,9,10,999]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Selected players must belong to the game roster"));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + rosterGameId
                + ",\"userName\":\"fielder\",\"players\":[24,25,26,27,28,29,30,31,32,33,35]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("User team must contain at least one wicketkeeper"));

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + rosterGameId
                + ",\"userName\":\"fielder\",\"players\":[23,34,31,32,33,42,43,44,24,25,26]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "User team must contain at least 5 bowlers and all-rounders"));

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2,\"outcome\":3}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Ball event outcome 3 is not supported"));
  }

  @Test
  void coversNotFoundAndDuplicateBranches() throws Exception {
    long gameId = createGame("IND", "PAK", 3, 5);

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"ghost\",\"players\":[1,2,3,4,5,6,7,8,9,10,11]}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User ghost does not exist"));

    createUser("user1");
    createUserTeam(gameId, "user1", "[1,2,3,4,5,6,7,8,9,10,11]");

    mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId
                + ",\"userName\":\"user1\",\"players\":[4,5,6,7,8,9,10,11,12,13,14]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "User team for user user1 already for game " + gameId));

    mockMvc.perform(get("/api/user-teams?gameId=9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("gameId 9999 does not exist"));

    mockMvc.perform(get("/api/users/9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("User 9999 does not exist"));

    long emptyGameId = createGame("SL", "BAN", 3, 5);
    mockMvc.perform(get("/api/user-teams?gameId={gameId}", emptyGameId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(
            "UserTeam does not exists for game " + emptyGameId));
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
    players.add(new Player(1L, "player1", PlayerType.BATTER));
    players.add(new Player(2L, "player2", PlayerType.BOWLER));

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
    long gameId = createGame("IND", "PAK", 3, 1);
    createUser("user1");
    createUserTeam(gameId, "user1", "[1,2,3,4,5,6,7,8,9,10,11]");

    mockMvc.perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk());

    for (int ball = 1; ball <= 5; ball++) {
      play(gameId, 1, 2, 1);
    }

    mockMvc.perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.ballsBowled").value(5));

    play(gameId, 1, 2, 1);

    mockMvc.perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.ballsBowled").value(6));

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2,\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + gameId + " already completed"));
  }

  @Test
  void automaticallyEndsGameWhenTenWicketsFall() throws Exception {
    long gameId = createGame("IND", "PAK", 3, 20);
    createUser("user1");
    createUserTeam(gameId, "user1", "[1,2,3,4,5,6,7,8,9,10,11]");

    mockMvc.perform(post("/api/games/{id}/start", gameId))
        .andExpect(status().isOk());

    for (int wicket = 1; wicket <= 9; wicket++) {
      play(gameId, 1, 2, -1);
    }

    mockMvc.perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.wickets").value(9));

    play(gameId, 1, 2, -1);

    mockMvc.perform(get("/api/games/{id}", gameId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.wickets").value(10))
        .andExpect(jsonPath("$.ballsBowled").value(10));

    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"batsman\":1,\"bowler\":2,\"outcome\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Game " + gameId + " already completed"));
  }

  private long createGame(String team1, String team2, int k, int overs) throws Exception {
    long team1StartId = nextPlayerId;
    long team2StartId = nextPlayerId + 11;
    nextPlayerId += 22;
    return idFrom(mockMvc.perform(post("/api/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(team1, team2, k, overs, team1StartId, team2StartId)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
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

  private long createUserTeam(long gameId, String userName, String playersJson) throws Exception {
    String body = "{\"gameId\":" + gameId + ",\"userName\":\"" + userName
        + "\",\"players\":" + playersJson + "}";
    return idFrom(mockMvc.perform(post("/api/user-teams")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private void play(long gameId, long batsman, long bowler, int outcome) throws Exception {
    String body = "{\"batsman\":" + batsman + ",\"bowler\":" + bowler
        + ",\"outcome\":" + outcome + "}";
    mockMvc.perform(post("/api/games/{id}/plays", gameId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
  }

  private long idFrom(String json) throws Exception {
    JsonNode node = objectMapper.readTree(json);
    return node.get("id").asLong();
  }

  private String gameRequestBody(String team1, String team2, Integer k, Integer overs,
      long team1StartId, long team2StartId) {
    StringBuilder builder = new StringBuilder();
    builder.append("{\"team1\":\"").append(team1)
        .append("\",\"team2\":\"").append(team2)
        .append("\",\"k\":").append(k);
    if (overs == null) {
      builder.append(",\"overs\":null");
    } else {
      builder.append(",\"overs\":").append(overs);
    }
    builder.append(",\"team1Players\":").append(playersJson(team1, team1StartId))
        .append(",\"team2Players\":").append(playersJson(team2, team2StartId))
        .append("}");
    return builder.toString();
  }

  private String playersJson(String teamName, long startId) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < 11; i++) {
      if (i > 0) {
        builder.append(',');
      }
      long playerId = startId + i;
      builder.append("{\"globalUniqueId\":").append(playerId)
          .append(",\"name\":\"").append(teamName).append("-").append(playerId)
          .append("\",\"type\":\"").append(playerType(i)).append("\"}");
    }
    builder.append(']');
    return builder.toString();
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
}
