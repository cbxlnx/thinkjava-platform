package com.thinkjava.platform.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expirationMillis}")
  private long expiration;

  public String generate(String subject){
    Date now = new Date();
    Date exp = new Date(now.getTime() + expiration);
    return Jwts.builder()
        .subject(subject)
        .issuedAt(now)
        .expiration(exp)
        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .compact();
  }

  public String validateAndGetSubject(String token){
    return Jwts.parser()
        .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
