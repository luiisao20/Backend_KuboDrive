package com.luisdev.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards {@code /api/internal/**} with a shared service token.
 *
 * A different credential from the user's JWT, on purpose: the JWT proves who a
 * person is, this proves the caller is a trusted machine on the container
 * network. Endpoints under this prefix have no user identity to reason about
 * at all — they serve every user's rows to a peer service.
 *
 * This filter is what makes the prefix safe: the security chain declares
 * {@code anyRequest().permitAll()}, so without it the internal feed would be
 * world-readable to anyone who can reach port 8080.
 */
@Component
public class InternalTokenFilter extends OncePerRequestFilter {

  public static final String INTERNAL_PATH_PREFIX = "/api/internal/";
  private static final String TOKEN_HEADER = "X-Internal-Token";

  private final String internalToken;

  public InternalTokenFilter(@Value("${internal.api.token}") String internalToken) {
    this.internalToken = internalToken;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    if (!matches(request.getHeader(TOKEN_HEADER))) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter()
          .write("{\"error\": \"No autorizado\", \"message\": \"Token interno invalido o ausente\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  /** Constant-time compare — a token check that leaks timing is not a check. */
  private boolean matches(String provided) {
    if (provided == null) {
      return false;
    }
    return MessageDigest.isEqual(
        provided.getBytes(StandardCharsets.UTF_8),
        internalToken.getBytes(StandardCharsets.UTF_8));
  }
}
