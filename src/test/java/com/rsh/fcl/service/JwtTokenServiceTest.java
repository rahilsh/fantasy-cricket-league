package com.rsh.fcl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rsh.fcl.model.User.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

  @Mock
  private JwtEncoder jwtEncoder;

  @Test
  void generateAccessTokenEncodesClaimsAndReturnsTokenValue() {
    JwtTokenService service = new JwtTokenService(jwtEncoder, 1800);
    Jwt jwt = Jwt.withTokenValue("signed-token")
        .header("alg", "HS256")
        .subject("alice")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(1800))
        .claim("roles", java.util.List.of("USER"))
        .build();
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);

    String token = service.generateAccessToken("alice", UserRole.USER);

    assertThat(token).isEqualTo("signed-token");
  }

  @Test
  void ttlReflectsConfiguredSeconds() {
    JwtTokenService service = new JwtTokenService(jwtEncoder, 1800);
    assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(1800L);
  }
}
