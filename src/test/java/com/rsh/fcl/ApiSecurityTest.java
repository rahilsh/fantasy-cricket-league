package com.rsh.fcl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rsh.fcl.model.CricketerType;
import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.CricketerRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.TeamRepository;
import com.rsh.fcl.repository.TournamentRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

  private static final AtomicInteger CRICKETER_SEQUENCE = new AtomicInteger();

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
  private TeamRepository teamRepository;

  @Autowired
  private TournamentRepository tournamentRepository;

  @Autowired
  private CricketerRepository cricketerRepository;

  @BeforeEach
  void cleanup() {
    ballEventRepository.deleteAll();
    userTeamRepository.deleteAll();
    gameRepository.deleteAll();
    teamRepository.deleteAll();
    tournamentRepository.deleteAll();
    cricketerRepository.deleteAll();
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
    Bootstrap bootstrap = bootstrap(superadminToken);

    mockMvc.perform(get("/api/games")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(bootstrap, 3, 5)))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + superadminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(bootstrap, 3, 5)))
        .andExpect(status().isCreated());
  }

  @Test
  void restrictsTournamentCricketerAndTeamApisToSuperadmin() throws Exception {
    String userToken = signupAndGetToken("user1", "password123");

    mockMvc.perform(post("/api/tournaments")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"T\"}"))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/cricketers?globalUniqueId=abc_xyz")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"X\",\"type\":\"BATTER\"}"))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/teams")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());

    // cricketers are readable by authenticated users
    mockMvc.perform(get("/api/cricketers")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk());
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

    JsonNode game = createGame(adminToken, 3);

    long user1TeamId = createTeam(user1Token, game.get("id").asLong(), validXi(game, 0));
    long user2TeamId = createTeam(user2Token, game.get("id").asLong(), validXi(game, 1));

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
    JsonNode game = createGame(superadminToken, 3);

    mockMvc.perform(get("/api/games/" + game.get("id").asLong())
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.team1Cricketers", org.hamcrest.Matchers.hasSize(11)))
        .andExpect(jsonPath("$.team2Cricketers", org.hamcrest.Matchers.hasSize(11)));
  }

  @Test
  void userManagesOwnTeamLifecycleThroughFilters() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String userToken = signupAndGetToken("user1", "password123");
    JsonNode game = createGame(superadminToken, 3);
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
        .andExpect(jsonPath("$.content[0].cricketers", org.hamcrest.Matchers.hasSize(11)));

    mockMvc.perform(put("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(gameId, validXi(game, 2))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cricketers", org.hamcrest.Matchers.hasSize(11)));

    mockMvc.perform(delete("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void superadminCanListAllUserTeamsThroughFilters() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String user1Token = signupAndGetToken("user1", "password123");
    String user2Token = signupAndGetToken("user2", "password123");
    JsonNode game = createGame(superadminToken, 3);
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

  // ---- helpers ----

  private JsonNode createGame(String token, int k) throws Exception {
    Bootstrap bootstrap = bootstrap(token);
    String json = mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(gameRequestBody(bootstrap, k, 5)))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(json);
  }

  /** Superadmin onboards a tournament with two valid teams ready to be matched in a game. */
  private Bootstrap bootstrap(String token) throws Exception {
    long tournamentId = idFrom(mockMvc.perform(post("/api/tournaments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Tournament\"}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
    long team1Id = onboardTeam(token, tournamentId, "IND");
    long team2Id = onboardTeam(token, tournamentId, "PAK");
    return new Bootstrap(tournamentId, team1Id, team2Id);
  }

  private long onboardTeam(String token, long tournamentId, String name) throws Exception {
    List<String> ids = new ArrayList<>();
    for (int offset = 0; offset < 11; offset++) {
      ids.add(createCricketer(token, typeForOffset(offset)));
    }
    String body = "{\"name\":\"" + name + "\",\"cricketers\":" + jsonArray(ids) + "}";
    return idFrom(mockMvc.perform(post("/api/tournaments/{id}/teams", tournamentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private String createCricketer(String token, CricketerType type) throws Exception {
    String id = nextCricketerId();
    mockMvc.perform(post("/api/cricketers?globalUniqueId={id}", id)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"C-" + id + "\",\"type\":\"" + type + "\"}"))
        .andExpect(status().isCreated());
    return id;
  }

  private long createTeam(String token, long gameId, List<String> cricketers) throws Exception {
    return idFrom(mockMvc.perform(post("/api/user-teams")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userTeamBody(gameId, cricketers)))
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
    return objectMapper.readTree(response).get("accessToken").asString();
  }

  private String loginAndGetToken(String userName, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("accessToken").asString();
  }

  private long idFrom(String json) {
    return objectMapper.readTree(json).get("id").asLong();
  }

  private String gameRequestBody(Bootstrap bootstrap, int k, int overs) {
    return "{\"tournamentId\":" + bootstrap.tournamentId
        + ",\"team1Id\":" + bootstrap.team1Id
        + ",\"team2Id\":" + bootstrap.team2Id
        + ",\"k\":" + k + ",\"overs\":" + overs + "}";
  }

  private String userTeamBody(long gameId, List<String> cricketers) {
    return "{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"cricketers\":"
        + jsonArray(cricketers) + "}";
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

  private static CricketerType typeForOffset(int offset) {
    if (offset == 0) {
      return CricketerType.WICKETKEEPER;
    }
    if (offset <= 4) {
      return CricketerType.BOWLER;
    }
    if (offset <= 7) {
      return CricketerType.ALLROUNDER;
    }
    return CricketerType.BATTER;
  }

  private static String nextCricketerId() {
    int n = CRICKETER_SEQUENCE.getAndIncrement();
    char[] letters = new char[6];
    for (int i = 5; i >= 0; i--) {
      letters[i] = (char) ('a' + (n % 26));
      n /= 26;
    }
    return new String(letters, 0, 3) + "_" + new String(letters, 3, 3);
  }

  /** Builds a valid XI (>=1 wicketkeeper, >=5 bowler/all-rounder) from a created game's roster. */
  private static List<String> validXi(JsonNode game, int variant) {
    List<String> all = new ArrayList<>();
    List<String> wicketkeepers = new ArrayList<>();
    List<String> pacemen = new ArrayList<>();
    for (String team : List.of("team1Cricketers", "team2Cricketers")) {
      for (JsonNode cricketer : game.get(team)) {
        String id = cricketer.get("globalUniqueId").asString();
        all.add(id);
        switch (cricketer.get("type").asString()) {
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

  private record Bootstrap(long tournamentId, long team1Id, long team2Id) {
  }
}
