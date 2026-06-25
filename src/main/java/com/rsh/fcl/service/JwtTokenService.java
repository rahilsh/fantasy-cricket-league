package com.rsh.fcl.service;

import com.rsh.fcl.model.User.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

  private final JwtEncoder jwtEncoder;
  private final Duration accessTokenTtl;

  public JwtTokenService(
      JwtEncoder jwtEncoder,
      @Value("${fcl.security.jwt.access-token-ttl-seconds:3600}") long accessTokenTtlSeconds) {
    this.jwtEncoder = jwtEncoder;
    this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds);
  }

  public String generateAccessToken(String userName, UserRole role) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("fantasy-cricket-league")
        .issuedAt(now)
        .expiresAt(now.plus(accessTokenTtl))
        .subject(userName)
        .claim("roles", List.of(role.name()))
        .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtl.getSeconds();
  }
}
