package com.rsh.fcl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  void enforcesAdminOnlyGameApis() throws Exception {
    String userToken = signupAndGetToken("user1", "password123");
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");

    mockMvc.perform(get("/api/games")
            .header("Authorization", "Bearer " + userToken))
        .andExpect(status().isForbidden());

    mockMvc.perform(get("/api/games")
            .header("Authorization", "Bearer " + superadminToken))
        .andExpect(status().isOk());
  }

  @Test
  void allowsOnlySuperadminToCreateAdminUsers() throws Exception {
    String superadminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String userToken = signupAndGetToken("user1", "password123");

    mockMvc.perform(post("/api/users")
            .header("Authorization", "Bearer " + superadminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"manager1\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("ADMIN"));

    mockMvc.perform(post("/api/users")
            .header("Authorization", "Bearer " + userToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"userName\":\"manager2\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void allowsUserToModifyOnlyOwnTeam() throws Exception {
    String adminToken = loginAndGetToken("fcl-admin", "fcl-admin-password");
    String user1Token = signupAndGetToken("user1", "password123");
    String user2Token = signupAndGetToken("user2", "password123");

    long gameId = idFrom(mockMvc.perform(post("/api/games")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"team1\":\"IND\",\"team2\":\"PAK\",\"k\":3}"))
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
