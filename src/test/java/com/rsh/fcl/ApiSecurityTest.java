package com.rsh.fcl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rsh.fcl.repository.BallEventRepository;
import com.rsh.fcl.repository.GameRepository;
import com.rsh.fcl.repository.UserRepository;
import com.rsh.fcl.repository.UserTeamRepository;
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

  @BeforeEach
  void cleanup() {
    ballEventRepository.deleteAll();
    userTeamRepository.deleteAll();
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
            .content("{\"team1\":\"IND\",\"team2\":\"PAK\",\"k\":3}"))
        .andExpect(status().isForbidden());

    mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + superadminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"team1\":\"IND\",\"team2\":\"PAK\",\"k\":3,\"overs\":5}"))
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

    long gameId = idFrom(mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"team1\":\"IND\",\"team2\":\"PAK\",\"k\":3,\"overs\":5}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());

    long user1TeamId = idFrom(mockMvc.perform(post("/api/user-teams")
            .header("Authorization", "Bearer " + user1Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"players\":[1,2,3]}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());

    long user2TeamId = idFrom(mockMvc.perform(post("/api/user-teams")
            .header("Authorization", "Bearer " + user2Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"players\":[4,5,6]}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());

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
    long gameId = createGame(superadminToken, "IND", "PAK", 3);

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
    long gameId = createGame(superadminToken, "IND", "PAK", 3);

    long teamId = createTeam(userToken, gameId, "[1,2,3]");

    mockMvc.perform(get("/api/user-teams")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$.content[0].userName").value("user1"));

    mockMvc.perform(get("/api/user-teams?gameId=" + gameId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].players", org.hamcrest.Matchers.hasSize(3)));

    mockMvc.perform(put("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"players\":[4,5,6,7]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.players", org.hamcrest.Matchers.hasSize(4)));

    mockMvc.perform(delete("/api/user-teams/" + teamId)
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isNoContent());
  }

  @Test
  void superadminCanListAllUserTeamsThroughFilters() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String user1Token = signupAndGetToken("user1", "password123");
    String user2Token = signupAndGetToken("user2", "password123");
    long gameId = createGame(superadminToken, "IND", "PAK", 3);
    createTeam(user1Token, gameId, "[1,2,3]");
    createTeam(user2Token, gameId, "[4,5,6]");

    mockMvc.perform(get("/api/user-teams")
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));

    mockMvc.perform(get("/api/user-teams?gameId=" + gameId)
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(2)));
  }

  private long createGame(String token, String team1, String team2, int k) throws Exception {
    return idFrom(mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"team1\":\"" + team1 + "\",\"team2\":\"" + team2 + "\",\"k\":" + k
                + ",\"overs\":5}"))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString());
  }

  private long createTeam(String token, long gameId, String playersJson) throws Exception {
    return idFrom(mockMvc.perform(post("/api/user-teams")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"gameId\":" + gameId + ",\"userName\":\"ignored\",\"players\":" + playersJson + "}"))
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
    JsonNode node = objectMapper.readTree(response);
    return node.get("accessToken").asText();
  }

  private String loginAndGetToken(String userName, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    JsonNode node = objectMapper.readTree(response);
    return node.get("accessToken").asText();
  }

  private long idFrom(String json) throws Exception {
    JsonNode node = objectMapper.readTree(json);
    return node.get("id").asLong();
  }
}
