package com.luisdev.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtProvider {

  @Value("${jwt.secret.key}")
  private String secretKey;

  public String generateToken(Authentication auth) {
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
    String roles = populateAuthorities(authorities);
    SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

    return Jwts.builder()
        .setIssuedAt(new Date())
        .setExpiration(new Date(new Date().getTime() + 86400000)) // 1 day
        .claim("email", auth.getName())
        .claim("authorities", roles)
        .signWith(key)
        .compact();
  }

  private String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
    Set<String> auths = new HashSet<>();
    for (GrantedAuthority grantedAuthority : authorities) {
      auths.add(grantedAuthority.getAuthority());
    }
    return String.join(",", auths);
  }
}
