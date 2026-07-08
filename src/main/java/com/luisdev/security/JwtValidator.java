package com.luisdev.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;

@Component
public class JwtValidator extends OncePerRequestFilter {

  @Value("${jwt.secret.key}")
  private String secretKey;

  private String recoverToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("jwt".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String jwt = recoverToken(request);

    if (jwt != null) {
      try {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt).getBody();

        String email = String.valueOf(claims.get("email"));
        String authorities = String.valueOf(claims.get("authorities"));
                
        java.util.List<org.springframework.security.core.GrantedAuthority> auths = 
            org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);
        
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null, auths);
        SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (Exception e) {
        // Ignore errors since endpoints are public for now
      }
    }

    filterChain.doFilter(request, response);
  }
}
