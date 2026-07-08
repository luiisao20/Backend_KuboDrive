package com.luisdev.utils;

import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import java.util.Arrays;

public class CookiesUtils {
  public static String extractTokenFromCookies(Cookie[] cookies) {
    if (cookies == null)
      return null;
    return Arrays.stream(cookies)
        .filter(c -> "jwt".equals(c.getName()))
        .map(c -> c.getValue())
        .findFirst()
        .orElse(null);
  }

  public static ResponseCookie createJwtCookie(String jwt, long maxAge) {
    return ResponseCookie.from("jwt", jwt)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(maxAge)
        .sameSite("None")
        .build();
  }

  public static ResponseCookie deleteJwtCookie() {
    return ResponseCookie.from("jwt", "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .maxAge(0)
        .sameSite("None")
        .build();
  }
}
