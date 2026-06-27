package com.rsh.fcl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.PlayerRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

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
  void rejectsUnauthenticatedRequests() throws Exception {
    mockMvc.perform(get("/api/games"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void allowsUsersToReadButOnlySuperadminToMutateGames() throws Exception {
    String userToken = signupAndGetToken("user1", "password123");
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");

    mockMvc.perform(get("/api/games")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody("IND", "PAK", 3, 5)))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + superadminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody("IND", "PAK", 3, 5)))
        .andExpect(status().isCreated());
  }

  @Test
  void restrictsUserManagementToSuperadmin() throws Exception {
    String userToken = signupAndGetToken("user1", "password123");
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");

    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/users")
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk());
  }

  @Test
  void allowsUserToModifyOnlyOwnTeam() throws Exception {
    String adminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String user1Token = signupAndGetToken("user1", "password123");
    String user2Token = signupAndGetToken("user2", "password123");

    JsonNode game = createGame(adminToken, "IND", "PAK", 3);
    long gameId = game.get("id").asLong();

    long user1TeamId = createTeam(user1Token, gameId, validXi(game, 0));
    long user2TeamId = createTeam(user2Token, gameId, validXi(game, 1));

    mockMvc.perform(get("/api/user-teams/" + user1TeamId)
            .header("Authorization", "Bearer " + user1Token))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/user-teams/" + user2TeamId)
            .header("Authorization", "Bearer " + user1Token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("User cannot access another user's team"));
  }

  @Test
  void userCanLogInAfterSignup() throws Exception {
    signupAndGetToken("alice", "password123");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"alice\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.accessToken").isNotEmpty());
  }

  @Test
  void rejectsInvalidCredentials() throws Exception {
    signupAndGetToken("alice", "password123");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"alice\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"ghost\",\"password\":\"password123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Invalid credentials"));
  }

  @Test
  void allowsUserToReadSingleGame() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String userToken = signupAndGetToken("user1", "password123");
    long gameId = createGame(superadminToken, "IND", "PAK", 3).get("id").asLong();

    mockMvc.perform(get("/api/games/" + gameId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team1").value("IND"))
        .andExpect(jsonPath("$.team2").value("PAK"));
  }

  @Test
  void userManagesOwnTeamLifecycleThroughFilters() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String userToken = signupAndGetToken("user1", "password123");
    JsonNode game = createGame(superadminToken, "IND", "PAK", 3);
    long gameId = game.get("id").asLong();

    long teamId = createTeam(userToken, gameId, validXi(game, 0));

    mockMvc.perform(get("/api/user-teams")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$.content[0].userName").value("user1"));

    mockMvc.perform(get("/api/user-teams?gameId=" + gameId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].players", org.hamcrest.Matchers.hasSize(11)));

    mockMvc.perform(put("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(gameId, validXi(game, 2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players", org.hamcrest.Matchers.hasSize(11)));

    mockMvc.perform(delete("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void superadminCanListAllUserTeamsThroughFilters() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String user1Token = signupAndGetToken("user1", "password123");
    String user2Token = signupAndGetToken("user2", "password123");
    JsonNode game = createGame(superadminToken, "IND", "PAK", 3);
    long gameId = game.get("id").asLong();
    createTeam(user1Token, gameId, validXi(game, 0));
    createTeam(user2Token, gameId, validXi(game, 1));

    mockMvc.perform(get("/api/user-teams")
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));

    mockMvc.perform(get("/api/user-teams?gameId=" + gameId)
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)));
  }

  private JsonNode createGame(String token, String team1, String team2, int k) throws Exception {
    String json = mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(team1, team2, k, 5)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(json);
  }

  private long createTeam(String token, long gameId, List<String> players) throws Exception {
    return idFrom(mockMvc.perform(post("/api/user-teams")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(gameId, players)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private String signupAndGetToken(String userName, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("accessToken").asText();
  }

  private String loginAndGetToken(String userName, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("accessToken").asText();
  }

  private long idFrom(String json) {
    return objectMapper.readTree(json).get("id").asLong();
  }

  private String gameRequestBody(String team1, String team2, int k, int overs) {
    return "{\"team1\":\"" + team1 + "\",\"team2\":\"" + team2 + "\",\"k\":" + k
        + ",\"overs\":" + overs
        + ",\"team1Players\":" + playersJson(team1)
        + ",\"team2Players\":" + playersJson(team2) + "}";
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

  private String userTeamBody(long gameId, List<String> players) {
    return "{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"players\":"
        + jsonArray(players) + "}";
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

  /** Builds a valid XI (>=1 wicketkeeper, >=5 bowler/all-rounder) from a created game's roster. */
  private static List<String> validXi(JsonNode game, int variant) {
    List<String> all = new ArrayList<>();
    List<String> wicketkeepers = new ArrayList<>();
    List<String> pacemen = new ArrayList<>();
    for (String team : List.of("team1Players", "team2Players")) {
      for (JsonNode player : game.get(team)) {
        String id = player.get("globalUniqueId").asString();
        all.add(id);
        switch (player.get("type").asString()) {
          case "WICKETKEEPER" -> wicketkeepers.add(id);
          case "BOWLER", "ALLROUNDER" -> pacemen.add(id);
          default -> { }
        }
      }
    }
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
