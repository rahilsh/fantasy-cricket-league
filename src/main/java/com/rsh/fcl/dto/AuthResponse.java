package com.rsh.fcl.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, String role) {
}
