package com.luisdev.controller;

import com.luisdev.domain.entity.User;
import com.luisdev.dto.AuthResponse;
import com.luisdev.dto.LoginRequest;
import com.luisdev.dto.RegisterRequest;
import com.luisdev.dto.UpdatePasswordRequest;
import com.luisdev.security.JwtProvider;
import com.luisdev.service.SessionService;
import com.luisdev.service.UserService;
import com.luisdev.utils.CookiesUtils;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

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
  private final com.luisdev.repository.FileMetadataRepository fileMetadataRepository;

  public AuthController(UserService userService, SessionService sessionService, JwtProvider jwtProvider, com.luisdev.repository.FileMetadataRepository fileMetadataRepository) {
    this.userService = userService;
    this.sessionService = sessionService;
    this.jwtProvider = jwtProvider;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
    User user = new User();
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPassword(request.getPassword());
    userService.registerUser(user);

    return ResponseEntity.ok().body(Map.of("message", "Ok"));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    String ip = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");

    Authentication auth = userService.login(request.getEmail(), request.getPassword(), ip);
    String token = jwtProvider.generateToken(auth);

    sessionService.createSession(request.getEmail(), token, ip, userAgent);

    ResponseCookie cookie = CookiesUtils.createJwtCookie(token, 86400); // 1 día

    User user = userService.findByEmail(request.getEmail());
    Long usedBytes = fileMetadataRepository.sumSizeBytesByOwnerId(user.getId());

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse("Login exitoso", request.getEmail(), user.getFirstName(), user.getLastName(), usedBytes));
  }

  @GetMapping("/validate")
  public ResponseEntity<AuthResponse> validate() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).body(new AuthResponse("No autorizado", null, null, null, null));
    }

    User user = userService.findByEmail(auth.getName());
    Long usedBytes = fileMetadataRepository.sumSizeBytesByOwnerId(user.getId());

    return ResponseEntity.ok(new AuthResponse("Sesión válida", auth.getName(), user.getFirstName(), user.getLastName(), usedBytes));
  }

  @PostMapping("/logout")
  public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
    String token = CookiesUtils.extractTokenFromCookies(request.getCookies());
    sessionService.deleteSession(token);

    ResponseCookie cookie = CookiesUtils.deleteJwtCookie();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(new AuthResponse("Logout exitoso", null, null, null, null));
  }

  @PutMapping("/update-password")
  public ResponseEntity<AuthResponse> updatePassword(@RequestBody UpdatePasswordRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return ResponseEntity.status(401).build();
    }

    userService.updatePassword(auth.getName(), request.getOldPassword(), request.getNewPassword());
    return ResponseEntity.ok(new AuthResponse("Contraseña actualizada exitosamente", auth.getName(), null, null, null));
  }
}
