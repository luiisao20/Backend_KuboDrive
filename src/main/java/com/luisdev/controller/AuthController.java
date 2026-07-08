package com.luisdev.controller;

import com.luisdev.domain.entity.User;
import com.luisdev.dto.AuthResponse;
import com.luisdev.dto.LoginRequest;
import com.luisdev.dto.RegisterRequest;
import com.luisdev.security.JwtProvider;
import com.luisdev.service.SessionService;
import com.luisdev.service.UserService;
import com.luisdev.utils.CookiesUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserService userService;
  private final SessionService sessionService;
  private final JwtProvider jwtProvider;

  public AuthController(UserService userService, SessionService sessionService, JwtProvider jwtProvider) {
    this.userService = userService;
    this.sessionService = sessionService;
    this.jwtProvider = jwtProvider;
  }

  @PostMapping("/register")
  public ResponseEntity<User> register(@RequestBody RegisterRequest request) {
    User user = new User();
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPassword(request.getPassword());

    return ResponseEntity.ok(userService.registerUser(user));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ip = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");

    Authentication auth = userService.login(request.getEmail(), request.getPassword(), ip);
    String token = jwtProvider.generateToken(auth);

    sessionService.createSession(request.getEmail(), token, ip, userAgent);

    ResponseCookie cookie = CookiesUtils.createJwtCookie(token, 86400); // 1 día

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse("Login exitoso", request.getEmail()));
  }

  @GetMapping("/validate")
  public ResponseEntity<AuthResponse> validate() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).body(new AuthResponse("No autorizado", null));
    }
    return ResponseEntity.ok(new AuthResponse("Sesión válida", auth.getName()));
  }

  @PostMapping("/logout")
  public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
    String token = CookiesUtils.extractTokenFromCookies(request.getCookies());
    sessionService.deleteSession(token);
    
    ResponseCookie cookie = CookiesUtils.deleteJwtCookie();
    
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse("Logout exitoso", null));
  }

  @PutMapping("/update-password")
  public ResponseEntity<AuthResponse> updatePassword(@RequestBody com.luisdev.dto.UpdatePasswordRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).build();
    }
    
    userService.updatePassword(auth.getName(), request.getOldPassword(), request.getNewPassword());
    return ResponseEntity.ok(new AuthResponse("Contraseña actualizada exitosamente", auth.getName()));
  }
}
