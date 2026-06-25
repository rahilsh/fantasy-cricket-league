package com.rsh.fcl;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void rejectsUnauthenticatedRequests() throws Exception {
    mockMvc.perform(get("/api/games"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void allowsAuthenticatedRequests() throws Exception {
    mockMvc.perform(get("/api/games")
            .with(httpBasic("fcl-admin", "fcl-password")))
        .andExpect(status().isOk());
  }
}
